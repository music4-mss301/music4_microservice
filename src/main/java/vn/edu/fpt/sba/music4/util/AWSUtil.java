package vn.edu.fpt.sba.music4.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

@Service
public class AWSUtil {

    private final S3Client s3Client;
    private final String bucketName;

    public AWSUtil(S3Client s3Client, @Value("${aws.s3.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    /**
     * Upload file lên S3, tự động đổi tên thành UUID và cho phép chọn thư mục (folderName)
     */
    public String uploadFile(MultipartFile file, String folderName) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Tạo mã UUID
        String uniqueId = UUID.randomUUID().toString();

        // Ghép folderName vào S3Key (VD: "images/1234abcd.jpg" hoặc "tracks/5678xyz.mp3")
        String s3Key = folderName + "/" + uniqueId + extension;

        // Build request bản V2
        PutObjectRequest putOb = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(file.getContentType())
                .build();

        // Thực thi upload
        s3Client.putObject(putOb, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        return s3Key; // Trả về Key để lưu Database
    }

    public String extractKeyFromUrl(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path.startsWith("/")) {
                return path.substring(1);
            }
            return path;
        } catch (URISyntaxException e) {
            return url;
        }
    }

    public void deleteFolder(String folderName) {
        if (!folderName.endsWith("/")) {
            folderName += "/";
        }

        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(folderName)
                .build();

        ListObjectsV2Response listRes;
        do {
            listRes = s3Client.listObjectsV2(listReq);
            for (S3Object s3Object : listRes.contents()) {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Object.key())
                        .build());
            }
            listReq = listReq.toBuilder().continuationToken(listRes.nextContinuationToken()).build();
        } while (listRes.isTruncated());
    }

    /**
     * Lấy luồng Stream từ S3, hỗ trợ HTTP 206 Partial Content cho Audio Player
     */
    public ResponseEntity<StreamingResponseBody> streamAudioFile(String s3Key, String rangeHeader) {
        try {
            // Build request gửi lên S3, nếu Client có gửi Range (tua nhạc) thì pass thẳng lên S3
            GetObjectRequest.Builder getObjectBuilder = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key);

            if (rangeHeader != null && !rangeHeader.isEmpty()) {
                getObjectBuilder.range(rangeHeader);
            }

            GetObjectRequest getObjectRequest = getObjectBuilder.build();

            // Gọi S3 để lấy luồng dữ liệu
            ResponseInputStream<GetObjectResponse> s3ObjectStream = s3Client.getObject(getObjectRequest);
            GetObjectResponse s3Response = s3ObjectStream.response();

            // Tạo StreamingResponseBody để đẩy thẳng byte từ S3 về Client mà không cần lưu vào RAM Backend
            StreamingResponseBody responseStream = outputStream -> {
                try (InputStream is = s3ObjectStream) {
                    byte[] buffer = new byte[1024 * 64]; // Đọc từng chunk 64KB
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                } catch (Exception e) {
                    // Client ngắt kết nối đột ngột (tắt tab, bấm next bài) sẽ quăng lỗi rác, có thể ignore log này
                }
            };

            // Lấy các thông số Header từ S3 trả về để báo cho Audio Player (Trình duyệt)
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add(HttpHeaders.CONTENT_TYPE, s3Response.contentType());
            responseHeaders.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(s3Response.contentLength()));

            if (s3Response.contentRange() != null) {
                responseHeaders.add(HttpHeaders.CONTENT_RANGE, s3Response.contentRange());
                responseHeaders.add(HttpHeaders.ACCEPT_RANGES, "bytes");
                return new ResponseEntity<>(responseStream, responseHeaders, HttpStatus.PARTIAL_CONTENT); // 206
            }

            return new ResponseEntity<>(responseStream, responseHeaders, HttpStatus.OK); // 200

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Lấy luồng Stream từ S3 nhưng khống chế giới hạn dung lượng (Dùng cho Guest nghe thử 30s)
     */
    public ResponseEntity<StreamingResponseBody> streamPreviewAudioFile(String s3Key, String rangeHeader) {
        try {
            // Giới hạn dung lượng trả về: 1MB (1,048,576 bytes)
            // Trung bình mp3 128kbps -> 320kbps thì 1MB sẽ tương đương khoảng 25s đến 45s nhạc.
            long MAX_PREVIEW_BYTES = 1048576L;

            long startByte = 0;
            long endByte = MAX_PREVIEW_BYTES - 1;

            // Xử lý nếu Client có truyền Header Range (Tua nhạc trong phạm vi 30s đó)
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] ranges = rangeHeader.substring(6).split("-");
                startByte = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    long requestedEnd = Long.parseLong(ranges[1]);
                    // Không cho phép endByte vượt quá giới hạn MAX_PREVIEW_BYTES
                    endByte = Math.min(requestedEnd, MAX_PREVIEW_BYTES - 1);
                }
            }

            // Nếu user cố tình tua vượt quá giới hạn 30s, trả về lỗi 416 (Requested Range Not Satisfiable)
            if (startByte >= MAX_PREVIEW_BYTES) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
            }

            // Ép S3 chỉ trả về đúng khúc byte trong vùng cho phép
            String s3Range = "bytes=" + startByte + "-" + endByte;

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .range(s3Range)
                    .build();

            ResponseInputStream<GetObjectResponse> s3ObjectStream = s3Client.getObject(getObjectRequest);
            GetObjectResponse s3Response = s3ObjectStream.response();

            StreamingResponseBody responseStream = outputStream -> {
                try (InputStream is = s3ObjectStream) {
                    byte[] buffer = new byte[1024 * 64];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                } catch (Exception e) {
                }
            };

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add(HttpHeaders.CONTENT_TYPE, s3Response.contentType());
            responseHeaders.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(endByte - startByte + 1));


            responseHeaders.add(HttpHeaders.CONTENT_RANGE, "bytes " + startByte + "-" + endByte + "/" + MAX_PREVIEW_BYTES);
            responseHeaders.add(HttpHeaders.ACCEPT_RANGES, "bytes");

            return new ResponseEntity<>(responseStream, responseHeaders, HttpStatus.PARTIAL_CONTENT);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Xóa một file bất kỳ trên S3 (Dùng dọn rác khi update avatar, đổi ảnh bìa, hoặc xóa nhạc)
     * @param fileUrlOrKey Đường dẫn URL đầy đủ hoặc S3 Key
     */
    public void deleteFile(String fileUrlOrKey) {
        // 1. Kiểm tra nếu không có file cũ thì bỏ qua (tránh lỗi null)
        if (fileUrlOrKey == null || fileUrlOrKey.trim().isEmpty()) {
            return;
        }

        try {
            // 2. Sử dụng lại hàm extractKey để đảm bảo luôn lấy được S3 Key chuẩn
            String s3Key = extractKeyFromUrl(fileUrlOrKey);

            // 3. Build request xóa object
            DeleteObjectRequest deleteOb = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            // 4. Thực thi lệnh xóa trên S3
            s3Client.deleteObject(deleteOb);

            System.out.println("Đã dọn dẹp thành công file cũ trên S3: " + s3Key);

        } catch (Exception e) {
            System.err.println("Cảnh báo: Không thể xóa file cũ trên S3 (" + fileUrlOrKey + ") - Lỗi: " + e.getMessage());
        }
    }
}