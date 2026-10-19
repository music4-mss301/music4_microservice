package vn.edu.fpt.mss.userservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** secret: chuỗi Base64 (>= 256 bit), dùng chung với api-gateway để verify. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long accessExpirationMs, long refreshExpirationDays) {
    public JwtProperties {
        accessExpirationMs = accessExpirationMs <= 0 ? 3_600_000 : accessExpirationMs;
        refreshExpirationDays = refreshExpirationDays <= 0 ? 7 : refreshExpirationDays;
    }
}
