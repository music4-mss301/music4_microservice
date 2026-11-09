package vn.edu.fpt.mss.contentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TrackBulkItem(
        @NotBlank(message = "Tên bài hát không được để trống")
        @Size(max = 100, message = "Tên bài hát không được vượt quá 100 ký tự")
        String title,

        @NotBlank(message = "Thiếu key file audio của bài hát")
        String audioFileKey,

        @NotNull(message = "Thiếu thời lượng bài hát")
        @Positive(message = "Thời lượng bài hát phải lớn hơn 0")
        Integer duration,

        Integer artistId,
        List<Integer> artistIds,

        @NotEmpty(message = "Mỗi bài hát phải thuộc ít nhất 1 thể loại")
        List<Integer> categoryIds) {
}
