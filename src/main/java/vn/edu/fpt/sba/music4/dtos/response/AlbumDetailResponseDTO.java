package vn.edu.fpt.sba.music4.dtos.response;

import java.time.LocalDate;
import java.util.List;

public record AlbumDetailResponseDTO(
        Integer id,
        String name,
        String img,
        LocalDate uploadDate,
        Integer trackTotal,
        ArtistResponseDTO artist, // Ca sĩ chủ quản của Album
        List<TrackResponseDTO> tracks // Danh sách các bài hát trong Album
) {}