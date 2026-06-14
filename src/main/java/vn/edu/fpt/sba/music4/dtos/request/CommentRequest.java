package vn.edu.fpt.sba.music4.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentRequest(
        @NotNull(message = "Track ID không được để trống") Integer trackId,
        Integer userId,
        Integer artistId,
        @NotBlank(message = "Nội dung bình luận không được trống") String content
) {}