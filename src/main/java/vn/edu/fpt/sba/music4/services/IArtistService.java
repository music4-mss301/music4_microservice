package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.dtos.request.RegistrationRequest;
import vn.edu.fpt.sba.music4.dtos.response.ArtistResponseDTO;
import vn.edu.fpt.sba.music4.entities.Artist;

import java.util.List;
import java.util.Optional;

public interface IArtistService {
    Artist getArtistById(Integer id);

    List<ArtistResponseDTO> getAllArtists();

    //    Optional<Artist> getByUsername (String username);
    Artist save(RegistrationRequest registrationRequest);
}
