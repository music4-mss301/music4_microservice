package vn.edu.fpt.mss.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;

/** username có thể là username hoặc email. */
public record LoginRequest(@NotBlank(message = "Username không được để trống") String username,
                           @NotBlank(message = "Mật khẩu không được để trống") String password) {
}
