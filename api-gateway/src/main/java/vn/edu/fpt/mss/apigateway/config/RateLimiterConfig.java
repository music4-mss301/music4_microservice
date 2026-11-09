package vn.edu.fpt.mss.apigateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.HttpStatusHolder;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import vn.edu.fpt.mss.apigateway.filter.JwtAuthenticationFilter;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Configuration
public class RateLimiterConfig {

    /**
     * KeyResolver kết hợp: Ưu tiên giới hạn theo User ID (nếu đã đăng nhập),
     * ngược lại giới hạn theo địa chỉ IP của Client.
     */
    @Bean
    @Primary
    public KeyResolver userOrIpKeyResolver() {
        return exchange -> {
            Object userId = exchange.getAttribute(JwtAuthenticationFilter.ATTR_USER_ID);
            if (userId != null) {
                return Mono.just("user:" + userId);
            }
            String userHeader = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (StringUtils.hasText(userHeader)) {
                return Mono.just("user:" + userHeader);
            }
            return Mono.just("ip:" + extractClientIp(exchange.getRequest()));
        };
    }

    /**
     * Ghi đè bean requestRateLimiterGatewayFilterFactory mặc định của Spring Cloud Gateway:
     * Mặc định Spring Cloud Gateway trả về HTTP 429 với body rỗng (0 byte).
     * Bean này trả về JSON chuẩn (kèm timestamp, status=429, message tiếng Việt,
     * path, requestId và header Retry-After) đồng nhất với toàn hệ thống.
     */
    @Bean
    @Primary
    @SuppressWarnings({"rawtypes", "unchecked"})
    public RequestRateLimiterGatewayFilterFactory requestRateLimiterGatewayFilterFactory(
            RateLimiter defaultRateLimiter,
            KeyResolver defaultKeyResolver,
            ObjectMapper objectMapper) {

        return new RequestRateLimiterGatewayFilterFactory(defaultRateLimiter, defaultKeyResolver) {
            @Override
            public String name() {
                return "RequestRateLimiter";
            }

            @Override
            public GatewayFilter apply(Config config) {
                KeyResolver resolver = config.getKeyResolver() != null ? config.getKeyResolver() : defaultKeyResolver;
                RateLimiter<Object> limiter = config.getRateLimiter() != null ? config.getRateLimiter() : defaultRateLimiter;
                boolean denyEmpty = config.getDenyEmptyKey() != null ? config.getDenyEmptyKey() : isDenyEmptyKey();
                HttpStatusHolder emptyKeyStatus = HttpStatusHolder.parse(
                        config.getEmptyKeyStatus() != null ? config.getEmptyKeyStatus() : getEmptyKeyStatusCode());

                return (exchange, chain) -> resolver.resolve(exchange)
                        .defaultIfEmpty("")
                        .flatMap(key -> {
                            if (!StringUtils.hasText(key)) {
                                if (denyEmpty) {
                                    ServerWebExchangeUtils.setResponseStatus(exchange, emptyKeyStatus);
                                    return exchange.getResponse().setComplete();
                                }
                                return chain.filter(exchange);
                            }

                            String routeId = config.getRouteId();
                            if (routeId == null) {
                                Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
                                routeId = route != null ? route.getId() : "default-route";
                            }
                            final String finalRouteId = routeId;

                            return limiter.isAllowed(finalRouteId, key).flatMap(rateResponse -> {
                                ServerHttpResponse httpResponse = exchange.getResponse();
                                for (Map.Entry<String, String> header : rateResponse.getHeaders().entrySet()) {
                                    httpResponse.getHeaders().add(header.getKey(), header.getValue());
                                }

                                if (rateResponse.isAllowed()) {
                                    return chain.filter(exchange);
                                }

                                String path = exchange.getRequest().getPath().value();
                                log.warn("RATE_LIMIT_EXCEEDED route={} key={} path={}", finalRouteId, key, path);

                                httpResponse.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                                httpResponse.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                                httpResponse.getHeaders().set("Retry-After", "1");

                                Map<String, Object> body = new LinkedHashMap<>();
                                body.put("timestamp", Instant.now().toString());
                                body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
                                body.put("error", HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
                                body.put("message", "Bạn đã gửi quá nhiều yêu cầu trong thời gian ngắn, vui lòng thử lại sau giây lát");
                                body.put("path", path);

                                String requestId = httpResponse.getHeaders().getFirst("X-Request-Id");
                                if (requestId != null) {
                                    body.put("requestId", requestId);
                                }

                                byte[] bytes;
                                try {
                                    bytes = objectMapper.writeValueAsBytes(body);
                                } catch (Exception e) {
                                    bytes = "Too Many Requests".getBytes(StandardCharsets.UTF_8);
                                }
                                DataBuffer buffer = httpResponse.bufferFactory().wrap(bytes);
                                return httpResponse.writeWith(Mono.just(buffer));
                            });
                        });
            }
        };
    }

    public static String extractClientIp(ServerHttpRequest request) {
        String xff = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            return normalizeLoopback(xff.split(",")[0].trim());
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return normalizeLoopback(remoteAddress.getAddress().getHostAddress());
        }
        return "unknown";
    }

    private static String normalizeLoopback(String ip) {
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}
