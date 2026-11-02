package vn.edu.fpt.mss.mediaservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import vn.edu.fpt.mss.mediaservice.config.StorageProperties;
import vn.edu.fpt.mss.mediaservice.exception.ResourceNotFoundException;
import vn.edu.fpt.mss.mediaservice.exception.ServiceUnavailableException;
import vn.edu.fpt.mss.mediaservice.service.StorageService;

import java.io.IOException;
import java.io.InputStream;

/** Port từ AWSUtil của monolith: upload, xóa, stream có Range (HTTP 206) để tua nhạc. */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private static final int BUFFER = 64 * 1024;

    private final S3Client s3Client;
    private final StorageProperties props;

    @Override
    public void put(String key, MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            s3Client.putObject(PutObjectRequest.builder().bucket(props.bucket()).key(key)
                    .contentType(file.getContentType()).build(), RequestBody.fromInputStream(in, file.getSize()));
        } catch (IOException | S3Exception e) {
            log.error("Upload S3 thất bại key={}: {}", key, e.getMessage());
            throw new ServiceUnavailableException("Không lưu được file lên kho lưu trữ");
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(props.bucket()).key(key).build());
        } catch (S3Exception e) {
            log.warn("Không xóa được object {}: {}", key, e.getMessage());
        }
    }

    @Override
    public ResponseEntity<StreamingResponseBody> stream(String key, String rangeHeader) {
        GetObjectRequest.Builder req = GetObjectRequest.builder().bucket(props.bucket()).key(key);
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            req.range(rangeHeader);
        }
        ResponseInputStream<GetObjectResponse> s3 = open(req.build());
        GetObjectResponse meta = s3.response();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, meta.contentType());
        headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(meta.contentLength()));
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        if (meta.contentRange() != null) {
            headers.set(HttpHeaders.CONTENT_RANGE, meta.contentRange());
            return new ResponseEntity<>(pipe(s3), headers, HttpStatus.PARTIAL_CONTENT);
        }
        return new ResponseEntity<>(pipe(s3), headers, HttpStatus.OK);
    }

    /** Guest chỉ nhận tối đa previewBytes đầu tiên của file (khoảng 30 giây). */
    @Override
    public ResponseEntity<StreamingResponseBody> streamPreview(String key, String rangeHeader) {
        long max = props.previewBytes();
        long start = 0;
        long end = max - 1;
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] parts = rangeHeader.substring(6).split("-");
            try {
                start = Long.parseLong(parts[0]);
                if (parts.length > 1 && !parts[1].isBlank()) {
                    end = Math.min(Long.parseLong(parts[1]), max - 1);
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
            }
        }
        if (start >= max || start > end) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
        }
        ResponseInputStream<GetObjectResponse> s3 = open(GetObjectRequest.builder()
                .bucket(props.bucket()).key(key).range("bytes=" + start + "-" + end).build());
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, s3.response().contentType());
        headers.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(s3.response().contentLength()));
        headers.set(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + max);
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        return new ResponseEntity<>(pipe(s3), headers, HttpStatus.PARTIAL_CONTENT);
    }

    @Override
    public String bucket() {
        return props.bucket();
    }

    private ResponseInputStream<GetObjectResponse> open(GetObjectRequest request) {
        try {
            return s3Client.getObject(request);
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("Không tìm thấy file");
        } catch (S3Exception e) {
            log.error("Đọc S3 thất bại key={}: {}", request.key(), e.getMessage());
            throw new ServiceUnavailableException("Kho lưu trữ đang lỗi, vui lòng thử lại sau");
        }
    }

    private StreamingResponseBody pipe(ResponseInputStream<GetObjectResponse> s3) {
        return out -> {
            try (InputStream in = s3) {
                byte[] buf = new byte[BUFFER];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
            } catch (IOException e) {
                log.debug("Client ngắt stream: {}", e.getMessage());
            }
        };
    }
}
