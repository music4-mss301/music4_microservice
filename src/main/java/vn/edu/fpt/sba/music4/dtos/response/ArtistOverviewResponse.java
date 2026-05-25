package vn.edu.fpt.sba.music4.dtos.response;

public record ArtistOverviewResponse(
        Long totalViews,
        Long totalFavorites,
        Long totalFollowers
) {
}
