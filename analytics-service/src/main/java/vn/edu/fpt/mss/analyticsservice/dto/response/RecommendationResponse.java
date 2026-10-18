package vn.edu.fpt.mss.analyticsservice.dto.response;

/** Giữ tên field như TrackSuggestResponse cũ của FE, thêm source: PERSONAL | TRENDING. */
public record RecommendationResponse(Integer trackId, String trackName, String img, Integer duration,
                                     String previewPath, long viewCount, Double matchScore, String source) {
}
