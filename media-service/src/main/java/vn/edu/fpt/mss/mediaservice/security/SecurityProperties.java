package vn.edu.fpt.mss.mediaservice.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * gatewaySecret: header X-Gateway-Secret do gateway gắn; để trống = không bắt buộc (chỉ dùng khi dev).
 * internalToken: header X-Internal-Token cho API /internal/** giữa các service.
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(String gatewaySecret, String internalToken,
                                 List<String> publicGetPaths, List<String> publicPaths) {
    public SecurityProperties {
        publicGetPaths = publicGetPaths == null ? List.of() : publicGetPaths;
        publicPaths = publicPaths == null ? List.of() : publicPaths;
    }
}
