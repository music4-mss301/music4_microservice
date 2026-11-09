package vn.edu.fpt.mss.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;
import vn.edu.fpt.mss.apigateway.config.GatewaySecurityProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Điểm xác thực DUY NHẤT của hệ thống (Zero-Trust Edge Security):
 * 1. Xóa mọi header danh tính client tự gửi (chống giả mạo X-User-Id, X-Artist-Id...).
 * 2. Chặn /internal/** từ bên ngoài; bảo vệ /actuator/** (chỉ mở health/info).
 * 3. Verify JWT (chữ ký + hạn + Redis Blacklist nếu bật). Path public: không bắt buộc token, nếu token hết hạn vẫn cho phép xem như khách.
 * 4. Gắn X-User-Id / X-Username / X-User-Role / X-Artist-Id + X-Gateway-Secret cho service phía sau.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    public static final String ATTR_USER_ID = "gateway.userId";
    private static final String ATTR_JWT_FILTERED = "gateway.jwt.filtered";

    private static final List<String> IDENTITY_HEADERS = List.of(
            "X-User-Id", "X-Username", "X-User-Role", "X-Artist-Id",
            "X-Gateway-Secret", "X-Internal-Token"
    );

    private final GatewaySecurityProperties props;
    private final ObjectMapper objectMapper;
    @Nullable
    private final ReactiveStringRedisTemplate redisTemplate;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final SecretKey key;

    public JwtAuthenticationFilter(
            GatewaySecurityProperties props,
            ObjectMapper objectMapper,
            @Autowired(required = false) @Nullable ReactiveStringRedisTemplate redisTemplate) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.jwtSecret()));
    }

    @Bean
    @Order(-100)
    public WebFilter jwtEdgeSecurityWebFilter() {
        return (exchange, chain) -> doFilter(exchange, chain::filter);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return doFilter(exchange, chain::filter);
    }

    private Mono<Void> doFilter(ServerWebExchange exchange, Function<ServerWebExchange, Mono<Void>> next) {
        if (exchange.getAttributes().putIfAbsent(ATTR_JWT_FILTERED, Boolean.TRUE) != null) {
            return next.apply(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 1. Chặn tuyệt đối các endpoint nội bộ giữa các microservice
        if (path.startsWith("/internal/")) {
            return reject(exchange, HttpStatus.NOT_FOUND, "Không tìm thấy endpoint");
        }

        // 2. Cho phép preflight CORS, fallback nội bộ và healthcheck cơ bản
        if (request.getMethod() == HttpMethod.OPTIONS
                || path.startsWith("/fallback")
                || path.equals("/actuator/health")
                || path.startsWith("/actuator/health/")
                || path.equals("/actuator/info")) {
            return next.apply(exchange);
        }

        boolean isActuatorAdminPath = path.startsWith("/actuator");
        boolean isPublic = !isActuatorAdminPath && isPublic(request.getMethod(), path);
        String token = extractToken(request, path);

        // Chuẩn bị request sạch: xóa toàn bộ header mạo danh và gắn Gateway Secret
        ServerHttpRequest.Builder builder = request.mutate()
                .headers(h -> IDENTITY_HEADERS.forEach(h::remove))
                .header("X-Gateway-Secret", props.gatewaySecret());

        if (token == null) {
            if (!isPublic) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập");
            }
            return next.apply(exchange.mutate().request(builder.build()).build());
        }

        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Object id = claims.get("id");
            Object role = claims.get("role");
            Object artistId = claims.get("artistId");

            if (id == null || role == null) {
                return handleInvalidToken(exchange, next, builder, isPublic, "Token thiếu thông tin người dùng");
            }

            String normalizedRole = String.valueOf(role).replace("ROLE_", "").toUpperCase();

            // Bảo vệ các endpoint /actuator/** (như /actuator/gateway/routes): chỉ cho phép ADMIN
            if (isActuatorAdminPath && !"ADMIN".equals(normalizedRole)) {
                return reject(exchange, HttpStatus.FORBIDDEN, "Chỉ ADMIN mới có quyền truy cập hệ thống giám sát");
            }

            String userIdStr = String.valueOf(id);
            String jti = claims.getId();

            return isTokenOrUserRevoked(jti, userIdStr)
                    .flatMap(revoked -> {
                        if (Boolean.TRUE.equals(revoked)) {
                            return handleInvalidToken(exchange, next, builder, isPublic,
                                    "Phiên đăng nhập đã bị thu hồi hoặc tài khoản bị khóa");
                        }

                        builder.header("X-User-Id", userIdStr)
                                .header("X-Username", claims.getSubject())
                                .header("X-User-Role", normalizedRole);
                        if (artistId != null) {
                            builder.header("X-Artist-Id", String.valueOf(artistId));
                        }

                        exchange.getAttributes().put(ATTR_USER_ID, userIdStr);
                        return next.apply(exchange.mutate().request(builder.build()).build());
                    });

        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT không hợp lệ tại path {}: {}", path, ex.getMessage());
            return handleInvalidToken(exchange, next, builder, isPublic, "Token không hợp lệ hoặc đã hết hạn");
        }
    }

    /**
     * Nếu path là Public (ví dụ GET /api/tracks/**) mà trình duyệt gửi kèm token cũ đã hết hạn,
     * cho phép hạ cấp xuống Anonymous (khách) thay vì chặn 401 làm gãy trải nghiệm người dùng.
     */
    private Mono<Void> handleInvalidToken(
            ServerWebExchange exchange,
            Function<ServerWebExchange, Mono<Void>> next,
            ServerHttpRequest.Builder cleanBuilder,
            boolean isPublic,
            String errorMessage) {
        if (isPublic) {
            cleanBuilder.headers(h -> h.remove(HttpHeaders.AUTHORIZATION));
            return next.apply(exchange.mutate().request(cleanBuilder.build()).build());
        }
        return reject(exchange, HttpStatus.UNAUTHORIZED, errorMessage);
    }

    /**
     * Kiểm tra thu hồi token hoặc khóa user trên Redis với cơ chế Fail-Open (timeout 150ms):
     * Nếu Redis không bật hoặc gặp sự cố mạng, tự động trả về false để không làm sập Gateway.
     */
    private Mono<Boolean> isTokenOrUserRevoked(@Nullable String jti, String userId) {
        if (!Boolean.TRUE.equals(props.redisBlacklistEnabled()) || redisTemplate == null) {
            return Mono.just(false);
        }
        Mono<Boolean> userBannedMono = redisTemplate.hasKey("auth:banned:user:" + userId);
        Mono<Boolean> jtiBlacklistedMono = StringUtils.hasText(jti)
                ? redisTemplate.hasKey("auth:blacklist:jti:" + jti)
                : Mono.just(false);

        return Mono.zip(userBannedMono, jtiBlacklistedMono)
                .map(tuple -> tuple.getT1() || tuple.getT2())
                .timeout(Duration.ofMillis(150))
                .onErrorResume(ex -> {
                    log.warn("Không thể kiểm tra Redis Blacklist (fallback stateless): {}", ex.getMessage());
                    return Mono.just(false);
                });
    }

    private boolean isPublic(@Nullable HttpMethod method, String path) {
        if (method == null) {
            return false;
        }
        for (String rule : props.publicPaths()) {
            int idx = rule.indexOf(':');
            if (idx <= 0) {
                continue;
            }
            String m = rule.substring(0, idx).trim();
            String pattern = rule.substring(idx + 1).trim();
            if (("*".equals(m) || method.name().equalsIgnoreCase(m)) && matcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private String extractToken(ServerHttpRequest request, String path) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        boolean queryAllowed = props.queryTokenPaths().stream().anyMatch(p -> matcher.match(p, path));
        String queryToken = request.getQueryParams().getFirst("token");
        return queryAllowed && StringUtils.hasText(queryToken) ? queryToken.trim() : null;
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", exchange.getRequest().getPath().value());
        String requestId = response.getHeaders().getFirst("X-Request-Id");
        if (requestId != null) {
            body.put("requestId", requestId);
        }
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = message.getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    /** Chạy trước các filter route (trước cả LoadBalancer). */
    @Override
    public int getOrder() {
        return -100;
    }
}
