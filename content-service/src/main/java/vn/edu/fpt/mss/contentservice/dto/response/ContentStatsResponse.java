package vn.edu.fpt.mss.contentservice.dto.response;

public record ContentStatsResponse(long tracks, long albums, long artists, long categories, long totalViews) {
}
