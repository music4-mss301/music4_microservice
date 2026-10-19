package vn.edu.fpt.mss.mediaservice.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.mss.mediaservice.dto.response.MediaUploadResponse;
import vn.edu.fpt.mss.mediaservice.service.MediaService;

import java.util.List;
import java.util.Map;

@Hidden
@RestController
@RequestMapping("/internal/media")
@RequiredArgsConstructor
public class InternalMediaController {

    private final MediaService mediaService;

    /** content-service gọi qua Feign (đã kiểm tra quyền ARTIST/ADMIN ở phía content). */
    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public MediaUploadResponse upload(@RequestPart("file") MultipartFile file, @RequestParam String type,
                                      @RequestParam(required = false) Integer uploadedBy) {
        return mediaService.upload(file, type, uploadedBy);
    }

    @DeleteMapping
    public Map<String, Integer> deleteByKeys(@RequestParam List<String> keys) {
        return Map.of("deleted", mediaService.deleteByKeys(keys));
    }
}
