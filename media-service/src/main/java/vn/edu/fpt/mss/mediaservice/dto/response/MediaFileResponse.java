package vn.edu.fpt.mss.mediaservice.dto.response;

import java.time.LocalDateTime;

public record MediaFileResponse(Long id, String objectKey, String originalFilename, String contentType,
                                String mediaType, Long fileSize, Integer duration, Integer uploadedBy,
                                LocalDateTime createdAt) {
}
