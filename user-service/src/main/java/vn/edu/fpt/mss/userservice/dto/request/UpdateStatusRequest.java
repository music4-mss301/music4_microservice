package vn.edu.fpt.mss.userservice.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull(message = "enabled không được để trống") Boolean enabled) {
}
