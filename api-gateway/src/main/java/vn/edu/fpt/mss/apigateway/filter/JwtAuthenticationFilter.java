package vn.edu.fpt.mss.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import vn.edu.fpt.mss.apigateway.config.GatewaySecurityProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Điểm xác thực DUY NHẤT của hệ thống:
 * 1. Xóa mọi header danh tính client tự gửi (chống giả mạo X-User-Id).
 * 2. Chặn /internal/** từ bên ngoài.
 * 3. Verify JWT (chữ ký + hạn). Path public: không bắt buộc token.
 * 4. Gắn X-User-Id / X-Username / X-User-Role + X-Gateway-Secret cho service phía sau.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final List<String> IDENTITY_HEADERS = List.of("X-User-Id", "X-Username", "X-User-Role",
            "X-Gateway-Secret", "X-Internal-Token");

    private final GatewaySecurityProperties props;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final SecretKey key;

    public JwtAuthenticationFilter(GatewaySecurityProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.jwtSecret()));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (path.startsWith("/internal/")) {
            return reject(exchange, HttpStatus.NOT_FOUND, "Không tìm thấy endpoint");
        }
        if (request.getMethod() == HttpMethod.OPTIONS || path.startsWith("/fallback") || path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        boolean isPublic = isPublic(request.getMethod(), path);
        String token = extractToken(request, path);

        ServerHttpRequest.Builder builder = request.mutate().headers(h -> IDENTITY_HEADERS.forEach(h::remove));
        builder.header("X-Gateway-Secret", props.gatewaySecret());

        if (token == null) {
            if (!isPublic) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập");
            }
            return chain.filter(exchange.mutate().request(builder.build()).build());
        }

        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            Object id = claims.get("id");
            Object role = claims.get("role");
            if (id == null || role == null) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Token thiếu thông tin người dùng");
            }
            builder.header("X-User-Id", String.valueOf(id))
                    .header("X-Username", claims.getSubject())
                    .header("X-User-Role", String.valueOf(role).replace("ROLE_", "").toUpperCase());
            return chain.filter(exchange.mutate().request(builder.build()).build());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT không hợp lệ: {}", ex.getMessage());
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Token không hợp lệ hoặc đã hết hạn");
        }
    }

    private boolean isPublic(HttpMethod method, String path) {
        for (String rule : props.publicPaths()) {
            int idx = rule.indexOf(':');
            String m = rule.substring(0, idx);
            String pattern = rule.substring(idx + 1);
            if (("*".equals(m) || method.name().equalsIgnoreCase(m)) && matcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private String extractToken(ServerHttpRequest request, String path) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        boolean queryAllowed = props.queryTokenPaths().stream().anyMatch(p -> matcher.match(p, path));
        String queryToken = request.getQueryParams().getFirst("token");
        return queryAllowed && StringUtils.hasText(queryToken) ? queryToken : null;
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
