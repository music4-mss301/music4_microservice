package vn.edu.fpt.mss.contentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateArtistProfileRequest(@NotNull Integer userId, @NotBlank String name, Boolean gender) {
}
