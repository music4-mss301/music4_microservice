package vn.edu.fpt.sba.music4.dtos.response;

import java.time.LocalDateTime;

public record NotificationResponseDTO(
        Integer id,
        Integer trackId,
        String trackName,
        String artistName,
        String content,
        Boolean isRead,
        LocalDateTime createdAt
) {}