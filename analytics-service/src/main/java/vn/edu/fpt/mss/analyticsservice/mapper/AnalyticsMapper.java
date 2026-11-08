package vn.edu.fpt.mss.analyticsservice.mapper;

import vn.edu.fpt.mss.analyticsservice.dto.client.Suggestion;
import vn.edu.fpt.mss.analyticsservice.dto.client.TrackSummary;
import vn.edu.fpt.mss.analyticsservice.dto.response.HistoryResponse;
import vn.edu.fpt.mss.analyticsservice.dto.response.RecommendationResponse;
import vn.edu.fpt.mss.analyticsservice.entity.History;

import java.util.List;

public final class AnalyticsMapper {

    private AnalyticsMapper() {
    }

    public static HistoryResponse toHistoryResponse(History h, TrackSummary track) {
        return new HistoryResponse(h.getId(), h.getTrackId(), h.getListenedAt(), h.getPlaybackPosition(), track);
    }

    public static RecommendationResponse fromSuggestion(Suggestion s) {
        return new RecommendationResponse(s.trackId(), s.trackId(), s.name(), s.name(), s.img(), s.duration(),
                s.previewPath(), s.previewPath(), s.viewCount(), s.score(), "PERSONAL", List.of());
    }

    public static RecommendationResponse fromTrending(TrackSummary t) {
        return new RecommendationResponse(t.id(), t.id(), t.name(), t.name(), t.img(), t.duration(),
                t.filePath(), t.previewPath(), t.viewCount(), null, "TRENDING", t.artists());
    }
}
