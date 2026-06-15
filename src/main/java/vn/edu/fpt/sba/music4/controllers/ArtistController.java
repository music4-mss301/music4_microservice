package vn.edu.fpt.sba.music4.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.request.ArtistRequestDTO;
import vn.edu.fpt.sba.music4.dtos.response.ArtistResponseDTO;
import vn.edu.fpt.sba.music4.services.IArtistService;

import java.util.List;

@RestController
@RequestMapping("/api/artists")
@RequiredArgsConstructor
public class ArtistController {
    private final IArtistService iArtistService;

    @GetMapping("")
    public List<ArtistResponseDTO> getAllArtist() {
        return iArtistService.getAllArtists();
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