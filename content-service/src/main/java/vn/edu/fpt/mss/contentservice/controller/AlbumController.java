package vn.edu.fpt.mss.contentservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.mss.contentservice.dto.request.AlbumRequest;
import vn.edu.fpt.mss.contentservice.dto.response.AlbumDetailResponse;
import vn.edu.fpt.mss.contentservice.dto.response.AlbumResponse;
import vn.edu.fpt.mss.contentservice.dto.response.PageResponse;
import vn.edu.fpt.mss.contentservice.service.AlbumService;

import java.util.List;

@Tag(name = "Albums")
@RestController
@RequestMapping("/api/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    @GetMapping
    public PageResponse<AlbumResponse> getAll(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return albumService.getAll(page, size);
    }

    @GetMapping("/{id}")
    public AlbumDetailResponse getDetail(@PathVariable Integer id) {
        return albumService.getDetail(id);
    }

    @GetMapping("/artist/{artistId}")
    public List<AlbumResponse> getByArtist(@PathVariable Integer artistId) {
        return albumService.getByArtist(artistId);
    }

    @PreAuthorize("hasAnyRole('ARTIST','ADMIN')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AlbumResponse> create(@Valid @RequestBody AlbumRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(albumService.create(request));
    }

    /** Tương thích FE cũ: form-data albumTitle + coverImageKey (file). */
    @PreAuthorize("hasAnyRole('ARTIST','ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AlbumResponse> createMultipart(
            @RequestParam("albumTitle") String albumTitle,
            @RequestPart(value = "coverImageKey", required = false) MultipartFile cover,
            @RequestParam(value = "artistId", required = false) Integer artistId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(albumService.createWithCover(albumTitle, cover, artistId));
    }

    @PreAuthorize("hasAnyRole('ARTIST','ADMIN')")
    @PutMapping("/{id}")
    public AlbumResponse update(@PathVariable Integer id, @Valid @RequestBody AlbumRequest request) {
        return albumService.update(id, request);
    }

    @PreAuthorize("hasAnyRole('ARTIST','ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        albumService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
