package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.dtos.request.ArtistRequestDTO;
import vn.edu.fpt.sba.music4.dtos.request.RegistrationRequest;
import vn.edu.fpt.sba.music4.dtos.response.ArtistResponseDTO;
import vn.edu.fpt.sba.music4.dtos.response.UserResponseDTO;
import vn.edu.fpt.sba.music4.entities.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface IArtistService {
    ArtistResponseDTO getArtistById(Integer id);

    Page<ArtistResponseDTO> getAllArtists(Pageable pageable);

    List<ArtistResponseDTO> getAllArtists();

    Artist save(RegistrationRequest registrationRequest);

    ArtistResponseDTO update(Integer artistId, ArtistRequestDTO artistRequest);

    long countTotalArtists();

    List<Map<String, Object>> getTop3Artists();

    List<UserResponseDTO> getFollowersByArtistId(Integer artistId);

}
