package vn.edu.fpt.sba.music4.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;
import vn.edu.fpt.sba.music4.entities.*;

import java.util.List;


public record SearchResultResponseDTO (Page<TrackResponseDTO> tracks,      // Thay bằng TrackDTO
                                       Page<AlbumResponseDTO> albums,      // Thay bằng AlbumDTO
                                       Page<ArtistResponseDTO> artists,    // Thay bằng ArtistDTO
                                       Page<PlaylistResponseDTO> playlists, // Thay bằng PlaylistDTO
                                       Page<CategoryResponseDTO> categories // Thay bằng CategoryDTO
){
}
