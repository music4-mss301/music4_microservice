package vn.edu.fpt.mss.contentservice.dto.client;

public record MediaUploadResponse(Long id, String objectKey, String contentType, String mediaType, Long fileSize,
                                  Integer duration) {
}
