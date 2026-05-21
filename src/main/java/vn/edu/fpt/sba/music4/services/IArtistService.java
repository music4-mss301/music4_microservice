package vn.edu.fpt.sba.music4.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.fpt.sba.music4.dto.response.ArtistDetailResponseDTO;

import java.util.List;

public interface IArtistService {
    List<ArtistDetailResponseDTO> findAll();
    Page<ArtistDetailResponseDTO> findAll(Pageable pageable);
}
