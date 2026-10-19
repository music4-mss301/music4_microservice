package vn.edu.fpt.mss.engagementservice.dto.response;

import java.time.LocalDateTime;

public record NotificationResponse(Integer id, String content, Integer trackId, Integer artistId, boolean isRead,
                                   LocalDateTime createdAt) {
}
