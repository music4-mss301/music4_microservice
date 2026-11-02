package vn.edu.fpt.mss.engagementservice.dto.response;

import java.time.LocalDateTime;

public record FollowResponse(Integer artistId, String artistName, String artistImg, LocalDateTime followedAt) {
}
