package vn.edu.fpt.mss.analyticsservice.dto.response;

import vn.edu.fpt.mss.analyticsservice.dto.client.TrackSummary;

import java.time.LocalDateTime;

public record HistoryResponse(Integer id, Integer trackId, LocalDateTime listenedAt, int playbackPosition,
                              TrackSummary track) {
}
