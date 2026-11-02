package vn.edu.fpt.mss.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * jwtSecret: cùng secret Base64 với user-service. gatewaySecret: gắn vào mọi request đi xuống service.
 * publicPaths: dạng "METHOD:/pattern" hoặc "*:/pattern"; không cần token nhưng nếu có token vẫn gắn danh tính.
 * queryTokenPaths: path được phép truyền token qua ?token= (thẻ audio không gửi được header).
 */
@ConfigurationProperties(prefix = "app.gateway")
public record GatewaySecurityProperties(String jwtSecret, String gatewaySecret, List<String> publicPaths,
                                        List<String> queryTokenPaths) {
    public GatewaySecurityProperties {
        publicPaths = publicPaths == null ? List.of() : publicPaths;
        queryTokenPaths = queryTokenPaths == null ? List.of() : queryTokenPaths;
    }
}
