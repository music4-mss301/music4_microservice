package vn.edu.fpt.mss.searchadminservice.dto.response;

import vn.edu.fpt.mss.searchadminservice.dto.client.StatsDtos;

/** Field null = service tương ứng đang không phản hồi (circuit breaker fallback). */
public record DashboardResponse(StatsDtos.UserStats users, StatsDtos.ContentStats content,
                                StatsDtos.EngagementStats engagement, StatsDtos.AnalyticsStats analytics,
                                ReindexStatus search) {
}
