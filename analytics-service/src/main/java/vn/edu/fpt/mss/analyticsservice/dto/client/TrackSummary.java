package vn.edu.fpt.mss.analyticsservice.dto.client;

import java.time.LocalDate;
import java.util.List;

public record TrackSummary(Integer id, String name, String img, Integer duration, String filePath, String previewPath,
                           long viewCount, LocalDate uploadDate, Integer albumId, String albumName,
                           List<ArtistRef> artists) {
    public record ArtistRef(Integer id, String name, String role) {
    }
}
