package vn.edu.fpt.mss.analyticsservice.dto.response;

import vn.edu.fpt.mss.analyticsservice.dto.client.TrackSummary.ArtistRef;
import java.util.List;

/** Giữ tên field như TrackSuggestResponse cũ của FE (trackId, trackName), đồng thời cung cấp id, name, artists cho FE mới. */
public record RecommendationResponse(Integer id, Integer trackId, String name, String trackName, String img, Integer duration,
                                     String filePath, String previewPath, long viewCount, Double matchScore, String source,
                                     List<ArtistRef> artists) {
}
