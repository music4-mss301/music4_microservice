package vn.edu.fpt.mss.engagementservice.dto.response;

import java.time.LocalDateTime;

/** Đúng thứ tự và tên trường của NotificationResponseDTO ở monolith (FE đã quen). */
public record NotificationResponse(Integer id, Integer trackId, String trackName, Integer albumId, String albumName,
                                   String artistName, String content, boolean isRead, LocalDateTime createdAt) {
}
