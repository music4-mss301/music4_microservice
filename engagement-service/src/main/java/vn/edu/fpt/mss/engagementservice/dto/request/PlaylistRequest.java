package vn.edu.fpt.mss.engagementservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaylistRequest(@NotBlank(message = "Tên playlist không được để trống") @Size(max = 100) String name,
                              @Size(max = 500, message = "Mô tả tối đa 500 ký tự") String description,
                              String img,
                              Boolean isPublic) {
}
