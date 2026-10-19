package vn.edu.fpt.mss.engagementservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** userId lấy từ token, không nhận từ body (tránh đánh giá hộ người khác). */
public record RatingRequest(@NotNull(message = "trackId không được để trống") Integer trackId,
                            @NotNull(message = "Điểm không được để trống")
                            @Min(value = 1, message = "Điểm tối thiểu là 1")
                            @Max(value = 5, message = "Điểm tối đa là 5") Integer ratingValue) {
}
