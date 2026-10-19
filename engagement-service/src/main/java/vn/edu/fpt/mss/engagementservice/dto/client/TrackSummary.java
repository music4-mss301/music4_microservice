package vn.edu.fpt.mss.engagementservice.dto.client;

import java.time.LocalDate;
import java.util.List;

/** Bản sao TrackResponse của content-service (chỉ field cần dùng). */
public record TrackSummary(Integer id, String name, String img, Integer duration, String filePath, String previewPath,
                           long viewCount, LocalDate uploadDate, Integer albumId, String albumName,
                           List<ArtistRef> artists) {
    public record ArtistRef(Integer id, String name, String role) {
    }
}
