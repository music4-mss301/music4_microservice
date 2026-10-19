package vn.edu.fpt.mss.engagementservice.dto.response;

import java.time.LocalDateTime;

public record CommentResponse(Integer id, Integer trackId, Integer userId, String userName, String userImg,
                              String content, LocalDateTime createdAt, boolean edited) {
}
