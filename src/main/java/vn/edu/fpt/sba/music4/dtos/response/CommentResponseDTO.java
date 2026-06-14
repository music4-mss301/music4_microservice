package vn.edu.fpt.sba.music4.dtos.response;

import java.time.LocalDateTime;

public record CommentResponseDTO(
        Integer commentId,
        String commenterName,
        String commenterImg,
        String commenterRole,
        String content,
        LocalDateTime createdAt
) {}