package vn.edu.fpt.mss.engagementservice.dto.response;

public record FavoriteToggleResponse(Integer trackId, boolean favorited, long totalFavorites) {
}
