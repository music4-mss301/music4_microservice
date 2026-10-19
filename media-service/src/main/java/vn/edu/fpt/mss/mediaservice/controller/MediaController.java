package vn.edu.fpt.mss.mediaservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import vn.edu.fpt.mss.mediaservice.dto.response.MediaFileResponse;
import vn.edu.fpt.mss.mediaservice.dto.response.MediaUploadResponse;
import vn.edu.fpt.mss.mediaservice.dto.response.PageResponse;
import vn.edu.fpt.mss.mediaservice.exception.BadRequestException;
import vn.edu.fpt.mss.mediaservice.service.MediaService;
import vn.edu.fpt.mss.mediaservice.service.StorageService;

@Tag(name = "Media")
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;
    private final StorageService storageService;

    @Operation(summary = "Upload file (type=images|tracks), trả objectKey để lưu ở service khác")
    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaUploadResponse> upload(@RequestPart("file") MultipartFile file,
                                                      @RequestParam String type) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaService.upload(file, type, null));
    }

    @GetMapping("/{id}")
    public MediaFileResponse get(@PathVariable Long id) {
        return mediaService.get(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public PageResponse<MediaFileResponse> list(@RequestParam(required = false) String type,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return mediaService.list(type, page, size);
    }

    @PutMapping(value = "/{id}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public MediaFileResponse replace(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        return mediaService.replace(id, file);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        mediaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Nghe đầy đủ: cần đăng nhập (gateway nhận JWT qua header hoặc ?token= cho thẻ audio). */
    @GetMapping("/stream")
    public ResponseEntity<StreamingResponseBody> stream(@RequestParam String key,
                                                        @RequestHeader(value = HttpHeaders.RANGE, required = false) String range) {
        requireAudioKey(key);
        return storageService.stream(key, range);
    }

    /** Nghe thử ~30 giây: công khai cho khách. */
    @GetMapping("/stream/preview")
    public ResponseEntity<StreamingResponseBody> preview(@RequestParam String key,
                                                         @RequestHeader(value = HttpHeaders.RANGE, required = false) String range) {
        requireAudioKey(key);
        return storageService.streamPreview(key, range);
    }

    /** Ảnh công khai (avatar, bìa). Chỉ cho đọc trong thư mục images/. */
    @GetMapping("/file")
    public ResponseEntity<StreamingResponseBody> image(@RequestParam String key) {
        if (!key.startsWith("images/") || key.contains("..")) {
            throw new BadRequestException("Chỉ được xem file ảnh");
        }
        return storageService.stream(key, null);
    }

    private void requireAudioKey(String key) {
        if (!key.startsWith("tracks/") || key.contains("..")) {
            throw new BadRequestException("Key audio không hợp lệ");
        }
    }
}
