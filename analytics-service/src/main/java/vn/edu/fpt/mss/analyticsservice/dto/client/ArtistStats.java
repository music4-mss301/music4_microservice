package vn.edu.fpt.mss.analyticsservice.dto.client;

import java.util.List;

public record ArtistStats(Integer artistId, Integer userId, List<Integer> trackIds, long totalViews) {
}
