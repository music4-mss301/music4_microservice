package vn.edu.fpt.mss.contentservice.dto.response;

/** Giữ tên field s3Key như API cũ để FE không phải sửa. */
public record UploadTempResponse(String s3Key, Integer duration) {
}
