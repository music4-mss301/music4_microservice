package vn.edu.fpt.mss.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank(message = "Refresh token không được để trống") String refreshToken) {
}
