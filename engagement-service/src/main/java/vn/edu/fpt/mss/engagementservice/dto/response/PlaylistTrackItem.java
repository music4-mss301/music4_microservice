package vn.edu.fpt.mss.engagementservice.dto.response;

import vn.edu.fpt.mss.engagementservice.dto.client.TrackSummary;

import java.time.LocalDateTime;

/** available=false khi content-service không trả được bài (đã xóa hoặc đang lỗi). */
public record PlaylistTrackItem(Integer trackId, LocalDateTime addedAt, boolean available, TrackSummary track) {
}
