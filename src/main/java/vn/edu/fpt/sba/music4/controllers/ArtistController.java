package vn.edu.fpt.sba.music4.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.request.ArtistRequestDTO;
import vn.edu.fpt.sba.music4.dtos.response.ArtistResponseDTO;
import vn.edu.fpt.sba.music4.services.IArtistService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequestMapping("/api/artists")
@RequiredArgsConstructor
public class ArtistController {
    private final IArtistService iArtistService;

    @GetMapping("")
    public Page<ArtistResponseDTO> getAllArtist(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        // Tạo đối tượng phân trang (bồ có thể thêm Sort nếu muốn sắp xếp theo ID tăng/giảm dần)
        Pageable pageable = PageRequest.of(page, size);

        return iArtistService.getAllArtists(pageable);
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArtistResponseDTO updateArtist(@PathVariable Integer id, @ModelAttribute ArtistRequestDTO artistRequest) {
        return iArtistService.update(id, artistRequest);
    }
    @GetMapping("/{id}")
    public ResponseEntity<ArtistResponseDTO> getArtistById(@PathVariable Integer id) {
        return ResponseEntity.ok(iArtistService.getArtistById(id));
    }
}