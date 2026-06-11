package vn.edu.fpt.sba.music4.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.sba.music4.services.IImageUploadService;

@RestController
@RequestMapping("/api/images")
@Tag(name = "Upload Ảnh (Images API)", description = "Các API dùng để up ảnh đại diện, ảnh bìa cho các đối tượng khác ngoài Track")
public class ImageUploadController {

    private final IImageUploadService imageUploadService;

    public ImageUploadController(IImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    @PostMapping(value = "/users/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cập nhật Avatar cho User")
    public ResponseEntity<String> uploadUserAvatar(
            @Parameter(description = "ID của User") @PathVariable Integer id,
            @Parameter(description = "File ảnh (.jpg, .png)") @RequestPart("avatar") MultipartFile file) {

        String s3Key = imageUploadService.uploadUserAvatar(id, file);
        return ResponseEntity.ok("Cập nhật avatar thành công! S3 Key: " + s3Key);
    }

    @PostMapping(value = "/artists/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cập nhật Avatar cho Ca sĩ")
    public ResponseEntity<String> uploadArtistAvatar(
            @Parameter(description = "ID của Ca sĩ") @PathVariable Integer id,
            @Parameter(description = "File ảnh (.jpg, .png)") @RequestPart("avatar") MultipartFile file) {

        String s3Key = imageUploadService.uploadArtistAvatar(id, file);
        return ResponseEntity.ok("Cập nhật avatar ca sĩ thành công! S3 Key: " + s3Key);
    }

    @PostMapping(value = "/albums/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cập nhật Ảnh bìa cho Album")
    public ResponseEntity<String> uploadAlbumCover(
            @Parameter(description = "ID của Album") @PathVariable Integer id,
            @Parameter(description = "File ảnh (.jpg, .png)") @RequestPart("cover") MultipartFile file) {

        String s3Key = imageUploadService.uploadAlbumCover(id, file);
        return ResponseEntity.ok("Cập nhật ảnh bìa album thành công! S3 Key: " + s3Key);
    }
}