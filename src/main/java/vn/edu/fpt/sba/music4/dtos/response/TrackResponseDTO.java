package vn.edu.fpt.sba.music4.dtos.response;

import java.time.LocalDate;

public record TrackResponseDTO(
        Integer id,
        String name,
        String title,
        String img,
        Integer duration,
        String filePath,
        String previewPath,
        Integer viewCount,
        LocalDate uploadDate,
        Integer playbackPosition
) {
}
