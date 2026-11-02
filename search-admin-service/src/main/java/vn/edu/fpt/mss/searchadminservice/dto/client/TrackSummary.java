package vn.edu.fpt.mss.searchadminservice.dto.client;

import java.time.LocalDate;

public record TrackSummary(Integer id, String name, String img, Integer duration, String previewPath, long viewCount,
                           LocalDate uploadDate, Integer albumId, String albumName) {
}
