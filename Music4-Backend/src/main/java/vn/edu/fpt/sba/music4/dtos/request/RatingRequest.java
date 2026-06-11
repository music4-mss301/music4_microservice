package vn.edu.fpt.sba.music4.dtos.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RatingRequest(
        @NotNull(message = "Track ID is required")
        Integer trackId,

        @NotNull(message = "User ID is required")
        Integer userId,

        @NotNull(message = "Rating value must not be null")
        @Min(value = 1, message = "Minimum rating value is 1")
        @Max(value = 5, message = "Maximum rating value is 5")
        Integer ratingValue
) {
}
