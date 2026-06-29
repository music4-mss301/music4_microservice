package vn.edu.fpt.sba.music4.dtos.response;

import java.util.List;

public record ArtistOverviewResponse(
        Long totalViews,
        Long totalFavorites,
        Long totalFollowers,
        Long totalComments,
        List<DailyAnalyticsResponse> chartData,
        List<TopTrackResponse> topTracks
) {
}
