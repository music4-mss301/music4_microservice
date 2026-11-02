package vn.edu.fpt.mss.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank(message = "Token không được để trống") String token,
        @NotBlank @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$", message = "Mật khẩu 8-64 ký tự, có cả chữ và số")
        String newPassword) {
}
