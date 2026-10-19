package vn.edu.fpt.mss.engagementservice.dto.response;

import java.time.LocalDateTime;

public record PlaylistResponse(Integer id, String name, String description, String img, boolean isPublic,
                               Integer userId, int trackCount, LocalDateTime createdAt) {
}
