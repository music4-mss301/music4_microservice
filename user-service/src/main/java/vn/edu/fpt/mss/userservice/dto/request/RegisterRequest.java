package vn.edu.fpt.mss.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Tên không được để trống") @Size(max = 100) String name,
        @NotBlank(message = "Email không được để trống") @Email(message = "Email không hợp lệ") @Size(max = 100) String email,
        @NotBlank(message = "Username không được để trống")
        @Pattern(regexp = "^[a-zA-Z0-9._]{3,50}$", message = "Username 3-50 ký tự, chỉ gồm chữ, số, dấu chấm, gạch dưới")
        String username,
        @NotBlank(message = "Mật khẩu không được để trống")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$", message = "Mật khẩu 8-64 ký tự, có cả chữ và số")
        String password,
        Boolean gender) {
}
