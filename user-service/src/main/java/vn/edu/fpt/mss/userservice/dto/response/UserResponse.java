package vn.edu.fpt.mss.userservice.dto.response;

import java.time.LocalDateTime;

public record UserResponse(Integer id, String name, String username, String email, String img, String role,
                           Integer artistId, Boolean gender, Boolean enabled, LocalDateTime createdAt) {
}
