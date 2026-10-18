package vn.edu.fpt.mss.analyticsservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SyncTimeRequest(@NotNull(message = "trackId không được để trống") Integer trackId,
                              @NotNull(message = "position không được để trống")
                              @Min(value = 0, message = "position phải >= 0")
                              @Max(value = 86400, message = "position không hợp lệ") Integer position) {
}
