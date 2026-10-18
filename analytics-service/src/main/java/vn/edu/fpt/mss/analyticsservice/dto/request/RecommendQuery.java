package vn.edu.fpt.mss.analyticsservice.dto.request;

import java.util.List;

/** Body gửi sang search-admin-service /internal/search/recommend. */
public record RecommendQuery(List<Integer> likedTrackIds, List<Integer> dislikedTrackIds,
                             List<Integer> excludeTrackIds, int size) {
}
