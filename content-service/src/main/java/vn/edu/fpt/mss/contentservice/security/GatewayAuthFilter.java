package vn.edu.fpt.mss.contentservice.security;

import vn.edu.fpt.mss.contentservice.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Service con KHÔNG tự parse JWT. Gateway verify JWT rồi gắn X-User-Id / X-Username / X-User-Role
 * kèm X-Gateway-Secret. Filter này:
 *  - /internal/**: bắt buộc X-Internal-Token (gọi giữa các service qua Feign).
 *  - Còn lại: bắt buộc X-Gateway-Secret (chặn gọi thẳng vào cổng service), rồi dựng Authentication từ header.
 */
@Component
@RequiredArgsConstructor
public class GatewayAuthFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USERNAME = "X-Username";
    public static final String HEADER_ROLE = "X-User-Role";
    public static final String HEADER_GATEWAY_SECRET = "X-Gateway-Secret";
    public static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";

    private static final List<String> OPEN_PREFIXES = List.of("/actuator", "/v3/api-docs", "/swagger-ui", "/error");

    private final SecurityProperties props;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path.startsWith("/internal/")) {
            if (!secretMatches(props.internalToken(), request.getHeader(HEADER_INTERNAL_TOKEN))) {
                reject(response, HttpStatus.FORBIDDEN, "Thiếu hoặc sai internal token", path);
                return;
            }
            var auth = new UsernamePasswordAuthenticationToken("internal", null,
                    List.of(new SimpleGrantedAuthority("ROLE_INTERNAL")));
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);
            return;
        }

        if (OPEN_PREFIXES.stream().anyMatch(path::startsWith)) {
            chain.doFilter(request, response);
            return;
        }

        if (StringUtils.hasText(props.gatewaySecret())
                && !secretMatches(props.gatewaySecret(), request.getHeader(HEADER_GATEWAY_SECRET))) {
            reject(response, HttpStatus.UNAUTHORIZED, "Request phải đi qua API Gateway", path);
            return;
        }

        String userId = request.getHeader(HEADER_USER_ID);
        String role = request.getHeader(HEADER_ROLE);
        if (StringUtils.hasText(userId) && StringUtils.hasText(role)) {
            try {
                AuthUser user = new AuthUser(Integer.valueOf(userId), request.getHeader(HEADER_USERNAME),
                        role.toUpperCase());
                var auth = new UsernamePasswordAuthenticationToken(user, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (NumberFormatException ex) {
                reject(response, HttpStatus.UNAUTHORIZED, "Header người dùng không hợp lệ", path);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean secretMatches(String expected, String actual) {
        if (!StringUtils.hasText(expected) || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private void reject(HttpServletResponse response, HttpStatus status, String message, String path)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiError.of(status, message, path));
    }
}
