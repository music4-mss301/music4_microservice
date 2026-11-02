package vn.edu.fpt.mss.mediaservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Credentials S3 chỉ nằm ở media-service. previewBytes: dung lượng cho guest nghe thử (~30s mp3 128kbps). */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String accessKey, String secretKey, String region, String bucket, long previewBytes) {
    public StorageProperties {
        previewBytes = previewBytes <= 0 ? 1_048_576L : previewBytes;
    }
}
