package vn.edu.fpt.mss.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@NotBlank @Email(message = "Email không hợp lệ") String email) {
}
