package vn.edu.fpt.mss.contentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TrackBulkItem(@NotBlank(message = "Tên bài hát không được để trống") @Size(max = 100) String title,
                            @NotBlank(message = "Thiếu key file audio") String audioFileKey,
                            @Positive(message = "Thời lượng phải > 0") Integer duration,
                            Integer artistId,
                            List<Integer> artistIds,
                            List<Integer> categoryIds) {
}
