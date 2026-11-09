package vn.edu.fpt.mss.contentservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.mss.contentservice.dto.request.ArtistUpdateRequest;
import vn.edu.fpt.mss.contentservice.dto.response.ArtistDetailResponse;
import vn.edu.fpt.mss.contentservice.dto.response.ArtistResponse;
import vn.edu.fpt.mss.contentservice.dto.response.PageResponse;
import vn.edu.fpt.mss.contentservice.service.ArtistService;

import java.util.List;

@Tag(name = "Artists")
@RestController
@RequestMapping("/api/artists")
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistService artistService;

    /** FE gọi /artists?page&size (phân trang, mặc định 12 như monolith) và /artists/all (toàn bộ). */
    @GetMapping("")
    public PageResponse<ArtistResponse> page(@RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "12") int size) {
        return artistService.search(null, page, size);
    }

    @GetMapping("/all")
    public List<ArtistResponse> getAll() {
        return artistService.getAll();
    }

    @GetMapping("/search")
    public PageResponse<ArtistResponse> search(@RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return artistService.search(keyword, page, size);
    }

    @PreAuthorize("hasRole('ARTIST')")
    @GetMapping("/me")
    public ArtistResponse me() {
        return artistService.getMine();
    }

    @GetMapping("/{id}")
    public ArtistDetailResponse getDetail(@PathVariable Integer id) {
        return artistService.getDetail(id);
    }

    @PreAuthorize("hasAnyRole('ARTIST','ADMIN')")
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ArtistResponse update(@PathVariable Integer id, @Valid @RequestBody ArtistUpdateRequest request) {
        return artistService.update(id, request);
    }

    @PreAuthorize("hasAnyRole('ARTIST','ADMIN')")
    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArtistResponse updateMultipart(
            @PathVariable Integer id,
            @RequestParam(value = "name", required = false) String name,
            @RequestPart(value = "img", required = false) MultipartFile img,
            @RequestPart(value = "cover", required = false) MultipartFile cover,
            @RequestParam(value = "gender", required = false) Boolean gender) {
        return artistService.updateMultipart(id, name, img, cover, gender);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        artistService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
