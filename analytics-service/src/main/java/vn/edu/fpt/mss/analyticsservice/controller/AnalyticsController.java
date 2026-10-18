package vn.edu.fpt.mss.analyticsservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.mss.analyticsservice.dto.response.*;
import vn.edu.fpt.mss.analyticsservice.service.RecommendationService;
import vn.edu.fpt.mss.analyticsservice.service.StatisticsService;

import java.util.List;

@Tag(name = "Analytics & Recommendations")
@RestController
@RequiredArgsConstructor
public class AnalyticsController {

    private final StatisticsService statisticsService;
    private final RecommendationService recommendationService;

    @GetMapping("/api/analytics/artist/{artistId}/overview")
    public ArtistOverviewResponse artistOverview(@PathVariable Integer artistId) {
        return statisticsService.artistOverview(artistId);
    }

    @GetMapping("/api/analytics/artist/{artistId}/views")
    public List<DailyCount> artistViews(@PathVariable Integer artistId,
                                        @RequestParam(defaultValue = "30") int days) {
        return statisticsService.artistDailyViews(artistId, days);
    }

    @GetMapping("/api/analytics/top-tracks")
    public List<TopTrackResponse> topTracks(@RequestParam(defaultValue = "7") int days,
                                            @RequestParam(defaultValue = "10") int limit) {
        return statisticsService.topTracks(days, limit);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/analytics/admin/overview")
    public AdminOverviewResponse adminOverview(@RequestParam(defaultValue = "30") int days) {
        return statisticsService.adminOverview(days);
    }

    /** Khách cũng xem được (trả trending). */
    @GetMapping("/api/recommendations")
    public List<RecommendationResponse> recommendations(@RequestParam(defaultValue = "10") int size) {
        return recommendationService.recommend(size);
    }
}
