package vn.edu.fpt.mss.searchadminservice.dto.client;

/** Số liệu thống kê lấy từ các service khác cho dashboard admin. */
public final class StatsDtos {
    private StatsDtos() {
    }

    public record UserStats(long totalUsers, long listeners, long artists, long admins) {
    }

    public record ContentStats(long tracks, long albums, long artists, long categories, long totalViews) {
    }

    public record EngagementStats(long playlists, long favorites, long follows, long ratings, long comments) {
    }

    public record AnalyticsStats(long totalPlays, long playsToday, long playsLast7Days) {
    }
}
