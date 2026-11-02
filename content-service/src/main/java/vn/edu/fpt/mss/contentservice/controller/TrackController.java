package vn.edu.fpt.mss.contentservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import vn.edu.fpt.mss.contentservice.dto.request.AlbumBulkRequest;
import vn.edu.fpt.mss.contentservice.dto.request.TrackUpdateRequest;
import vn.edu.fpt.mss.contentservice.dto.request.TrackUploadRequest;
import vn.edu.fpt.mss.contentservice.dto.response.*;
import vn.edu.fpt.mss.contentservice.service.TrackService;

import java.net.URI;
import java.util.List;

@Tag(name = "Tracks")
@RestController
@RequestMapping("/api/tracks")
@RequiredArgsConstructor
public class TrackController {

    private final TrackService trackService;

    @GetMapping
    public PageResponse<TrackResponse> getAll(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestParam(defaultValue = "id") String sort) {
        return trackService.getAll(page, size, sort);
    }

    @GetMapping("/{id}")
    public TrackDetailResponse getDetail(@PathVariable Integer id) {
        return trackService.getDetail(id);
    }

    @GetMapping("/album/{albumId}")
    public List<TrackResponse> getByAlbum(@PathVariable Integer albumId) {
        return trackService.getByAlbum(albumId);
    }

    @GetMapping("/artist/{artistId}")
    public List<TrackResponse> getByArtist(@PathVariable Integer artistId) {
        return trackService.getByArtist(artistId);
    }

    @GetMapping("/trending")
    public List<TrackResponse> trending(@RequestParam(defaultValue = "10") int limit) {
        return trackService.getTrending(limit);
    }

    @Operation(summary = "Upload bài hát kèm file (form-data: audioFile, coverImage, title, albumId, categoryIds, artistIds)")
    @PreAuthorize("hasAnyRole('ARTIST','ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrackDetailResponse> upload(@RequestPart("audioFile") MultipartFile audioFile,
                                                      @RequestPart("coverImage") MultipartFile coverImage,
                                                      @Valid @ModelAttribute TrackUploadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trackService.upload(audioFile, coverImage, request));
    }

    @Operation(summary = "Upload file lẻ trước (type=images|tracks), trả s3Key + duration")
    @PreAuthorize("hasAnyRole('ARTIST','ADMIN')")
    @PostMapping(value = "/upload-temp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadTempResponse uploadTemp(@RequestPart("file") MultipartFile file, @RequestParam String type) {
        return trackService.uploadTemp(file, type);
    }

    @PreAuthorize("hasAnyRole('ARTIST','ADMIN')")
    @PostMapping("/bulk-json")
    public ResponseEntity<List<TrackResponse>> saveBulk(@Valid @RequestBody AlbumBulkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trackService.saveBulk(request));
    }

    /** FE cũ gọi cả PUT /tracks/{id} và PUT /tracks/update/{id}. */
    @PreAuthorize("hasAnyRole('ARTIST','ADMIN')")
    @PutMapping({"/{id}", "/update/{id}"})
    public TrackDetailResponse update(@PathVariable Integer id, @Valid @RequestBody TrackUpdateRequest request) {
        return trackService.update(id, request);
    }

    @PreAuthorize("hasAnyRole('ARTIST','ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        trackService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Giữ URL stream cũ cho FE. File nằm ở media-service nên trả 302 sang /api/media/stream.
     * Bản đầy đủ cần đăng nhập (gateway nhận token qua query ?token= vì thẻ audio không gửi header).
     */
    @GetMapping("/stream/{id}")
    public ResponseEntity<Void> stream(@PathVariable Integer id, @RequestParam(required = false) String token) {
        return redirect("/api/media/stream", trackService.resolveStreamKey(id, false), token);
    }

    @GetMapping("/stream/preview/{id}")
    public ResponseEntity<Void> preview(@PathVariable Integer id) {
        return redirect("/api/media/stream/preview", trackService.resolveStreamKey(id, true), null);
    }

    private ResponseEntity<Void> redirect(String path, String key, String token) {
        var builder = UriComponentsBuilder.fromPath(path).queryParam("key", key);
        if (token != null && !token.isBlank()) {
            builder.queryParam("token", token);
        }
        URI location = builder.encode().build().toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }
}
