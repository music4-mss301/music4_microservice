package vn.edu.fpt.mss.engagementservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommentRequest(@NotNull(message = "trackId không được để trống") Integer trackId,
                             @NotBlank(message = "Nội dung không được để trống")
                             @Size(max = 500, message = "Bình luận tối đa 500 ký tự") String content) {
}
