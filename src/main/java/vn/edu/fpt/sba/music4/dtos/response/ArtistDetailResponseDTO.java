package vn.edu.fpt.sba.music4.dtos.response;

import java.util.List;

public record ArtistDetailResponseDTO(
        Integer id,
        String name,
        List<AlbumResponseDTO> albums,
        String username
)
{ }
