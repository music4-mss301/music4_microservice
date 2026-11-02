package vn.edu.fpt.mss.mediaservice.dto.response;

/** Trả cho service gọi nội bộ (content). s3Key giữ để tương thích FE cũ (= objectKey). */
public record MediaUploadResponse(Long id, String objectKey, String s3Key, String contentType, String mediaType,
                                  Long fileSize, Integer duration) {
}
