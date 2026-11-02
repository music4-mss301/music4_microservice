package vn.edu.fpt.mss.analyticsservice.dto.request;

import jakarta.validation.constraints.NotNull;

/** userId lấy từ token (khách = null), không nhận từ body. */
public record PlayRequest(@NotNull(message = "trackId không được để trống") Integer trackId) {
}
