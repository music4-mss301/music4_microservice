package vn.edu.fpt.sba.music4.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.sba.music4.dtos.response.ArtistDetailResponseDTO;
import vn.edu.fpt.sba.music4.dtos.response.PageResponseDTO;
import vn.edu.fpt.sba.music4.services.IArtistService;

@RestController
@RequestMapping("/api/artists")
public class ArtistController {
    private final IArtistService iArtistService;
    public ArtistController(IArtistService iArtistService) {
        this.iArtistService = iArtistService;
    }

    @GetMapping("")
    public PageResponseDTO<ArtistDetailResponseDTO> artistList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ArtistDetailResponseDTO> pageRes = iArtistService.findAll(pageable);
        return PageResponseDTO.of(pageRes);
    }
}