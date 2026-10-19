package vn.edu.fpt.mss.contentservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
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

    /** FE gọi cả /artists và /artists/all. */
    @GetMapping({"", "/all"})
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
    @PatchMapping("/{id}")
    public ArtistResponse update(@PathVariable Integer id, @Valid @RequestBody ArtistUpdateRequest request) {
        return artistService.update(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        artistService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
