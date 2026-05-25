package vn.edu.fpt.sba.music4.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ArtistRequestDTO(
        @NotBlank
        @Size(min = 3, max = 100, message = "name min 3 character")
        String name) {
}
