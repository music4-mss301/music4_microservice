package vn.edu.fpt.mss.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** PATCH: field null = giữ nguyên. img là object key trả về từ media-service. */
public record UpdateProfileRequest(@Size(min = 1, max = 100) String name,
                                   @Email(message = "Email không hợp lệ") @Size(max = 100) String email,
                                   String img,
                                   Boolean gender) {
}
