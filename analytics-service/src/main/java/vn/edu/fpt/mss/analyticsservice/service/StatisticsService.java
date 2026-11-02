package vn.edu.fpt.mss.analyticsservice.service;

import vn.edu.fpt.mss.analyticsservice.dto.response.*;

import java.util.List;

public interface StatisticsService {
    ArtistOverviewResponse artistOverview(Integer artistId);

    List<DailyCount> artistDailyViews(Integer artistId, int days);

    AdminOverviewResponse adminOverview(int days);

    List<TopTrackResponse> topTracks(int days, int limit);

    AnalyticsStatsResponse stats();
}
