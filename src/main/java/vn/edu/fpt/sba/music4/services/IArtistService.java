package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.dtos.request.ArtistRequestDTO;
import vn.edu.fpt.sba.music4.dtos.request.RegistrationRequest;
import vn.edu.fpt.sba.music4.dtos.response.ArtistResponseDTO;
import vn.edu.fpt.sba.music4.entities.Artist;

import java.util.List;

public interface IArtistService {
    Artist getArtistById(Integer id);

    List<ArtistResponseDTO> getAllArtists();

    Artist save(RegistrationRequest registrationRequest);

    ArtistResponseDTO update(Integer artistId, ArtistRequestDTO artistRequest);
}
