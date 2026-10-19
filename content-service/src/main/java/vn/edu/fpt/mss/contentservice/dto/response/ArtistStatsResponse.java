package vn.edu.fpt.mss.contentservice.dto.response;

import java.util.List;

public record ArtistStatsResponse(Integer artistId, Integer userId, List<Integer> trackIds, long totalViews) {
}
