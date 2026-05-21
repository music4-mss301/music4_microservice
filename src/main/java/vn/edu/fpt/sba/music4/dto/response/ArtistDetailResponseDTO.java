package vn.edu.fpt.sba.music4.dto.response;

import vn.edu.fpt.sba.music4.entities.Playlist;

import java.util.List;

public record ArtistDetailResponseDTO(
        Integer id,
        String name,
        List<AlbumResponseDTO> albums,
        String username
)
{ }
