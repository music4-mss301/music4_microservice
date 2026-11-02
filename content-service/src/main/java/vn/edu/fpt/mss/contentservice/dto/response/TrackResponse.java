package vn.edu.fpt.mss.contentservice.dto.response;

import java.time.LocalDate;
import java.util.List;

public record TrackResponse(Integer id, String name, String img, Integer duration, String filePath,
                            String previewPath, long viewCount, LocalDate uploadDate, Integer albumId,
                            String albumName, List<TrackArtistInfo> artists) {
}
