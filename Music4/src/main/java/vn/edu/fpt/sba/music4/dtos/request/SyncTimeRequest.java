package vn.edu.fpt.sba.music4.dtos.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SyncTimeRequest(
        @NotNull(message = "Track ID is required")
        Integer trackId,

        @NotNull(message = "User ID is required")
        Integer userId,

        @NotNull(message = "Time is required")
        @Min(value = 0, message = "Time must be greater than 0")
        Integer position
) {
}
