package vn.edu.fpt.mss.analyticsservice.dto.response;

public record ArtistOverviewResponse(Integer artistId, long totalViews, long totalFavorites, long totalFollowers,
                                     int totalTracks, long playsLast30Days) {
}
