package vn.edu.fpt.mss.engagementservice.service;

import vn.edu.fpt.mss.engagementservice.dto.response.ArtistEngagementStats;
import vn.edu.fpt.mss.engagementservice.dto.response.EngagementStatsResponse;
import vn.edu.fpt.mss.engagementservice.dto.response.UserSignalsResponse;

import java.util.List;

/** Dữ liệu tổng hợp cho analytics/search-admin + dọn dẹp khi nhận event. */
public interface InsightService {
    UserSignalsResponse signals(Integer userId);

    ArtistEngagementStats artistStats(Integer artistId, List<Integer> trackIds);

    EngagementStatsResponse stats();

    void cleanupTrack(Integer trackId);

    void cleanupArtist(Integer artistId);

    void cleanupUser(Integer userId);
}
