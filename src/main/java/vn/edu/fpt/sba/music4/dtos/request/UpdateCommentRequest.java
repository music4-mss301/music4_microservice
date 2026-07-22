package vn.edu.fpt.sba.music4.dtos.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommentRequest(
        @NotBlank(message = "Nội dung bình luận không được trống")
        String content
) {}