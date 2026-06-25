package vn.edu.fpt.sba.music4.dtos.response;

import java.time.LocalDate;
import java.util.List;

public record TrackResponseDTO(
        Integer id,
        String name,
        String albumName,
        String img,
        Integer duration,
        String filePath,
        String previewPath,
        Integer viewCount,
        LocalDate uploadDate,
        Integer playbackPosition,
        List<TrackArtistInfo> artists
) {
}
