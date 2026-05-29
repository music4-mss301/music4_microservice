package vn.edu.fpt.sba.music4.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.sba.music4.dtos.response.ArtistResponseDTO;
import vn.edu.fpt.sba.music4.entities.Artist;
import vn.edu.fpt.sba.music4.services.IArtistService;

import java.util.List;

@RestController
@RequestMapping("/api/artists")
public class ArtistController {
    private final IArtistService iArtistService;
    public ArtistController(IArtistService iArtistService) {
        this.iArtistService = iArtistService;
    }

    @GetMapping("")
    public List<ArtistResponseDTO> getAllArtist() {
        return iArtistService.getAllArtists();
    }

}