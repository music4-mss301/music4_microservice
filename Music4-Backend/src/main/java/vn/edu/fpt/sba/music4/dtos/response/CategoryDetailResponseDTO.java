package vn.edu.fpt.sba.music4.dtos.response;

import java.util.List;

public record CategoryDetailResponseDTO(
        Integer id,
        String name,
        List<TrackResponseDTO> tracks
) {}
