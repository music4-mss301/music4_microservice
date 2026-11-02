package vn.edu.fpt.mss.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateRoleRequest(
        @NotBlank @Pattern(regexp = "(?i)LISTENER|ARTIST|ADMIN", message = "Role phải là LISTENER, ARTIST hoặc ADMIN")
        String role) {
}
