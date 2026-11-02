package vn.edu.fpt.mss.engagementservice.dto.request;

import jakarta.validation.constraints.NotBlank;

/** img là object key từ media-service. */
public record PlaylistImageRequest(@NotBlank(message = "Ảnh không được để trống") String img) {
}
