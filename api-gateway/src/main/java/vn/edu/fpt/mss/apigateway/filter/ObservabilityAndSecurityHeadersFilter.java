package vn.edu.fpt.mss.apigateway.filter;

import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework. core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;
import vn.edu.fpt.mss.apigateway.config.RateLimiterConfig;

import java.util.UUID;
import java.util.function.Function;

/**
 * Filter chạy đầu tiên (Ordered.HIGHEST_PRECEDENCE + 10) cho toàn bộ HTTP request:
 * 1. Đảm bảo mọi request đều có X-Request-Id / X-Trace-Id xuyên suốt.
 * 2. Gắn các OWASP Security Headers bảo vệ trình duyệt.
 * 3. Ghi Structured Access Log (Method, Path, Status, Latency ms, User, IP) cho ELK Stack.
 */
@Slf4j
@Component
public class ObservabilityAndSecurityHeadersFilter implements GlobalFilter, Ordered {

    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String ATTR_OBS_FILTERED = "gateway.obs.filtered";

    @Nullable
    private final Tracer tracer;

    public ObservabilityAndSecurityHeadersFilter(@Autowired(required = false) @Nullable Tracer tracer) {
        this.tracer = tracer;
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    public WebFilter observabilityWebFilter() {
        return (exchange, chain) -> doFilter(exchange, chain::filter);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return doFilter(exchange, chain::filter);
    }

    private Mono<Void> doFilter(ServerWebExchange exchange, Function<ServerWebExchange, Mono<Void>> next) {
        if (exchange.getAttributes().putIfAbsent(ATTR_OBS_FILTERED, Boolean.TRUE) != null) {
            return next.apply(exchange);
        }

        long startTimeNs = System.nanoTime();
        ServerHttpRequest request = exchange.getRequest();

        String requestId = request.getHeaders().getFirst(HEADER_REQUEST_ID);
        if (!StringUtils.hasText(requestId)) {
            requestId = (tracer != null && tracer.currentSpan() != null)
                    ? tracer.currentSpan().context().traceId()
                    : UUID.randomUUID().toString();
        }
        final String finalRequestId = requestId;

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(HEADER_REQUEST_ID, finalRequestId)
                .build();

        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders initialHeaders = response.getHeaders();
        initialHeaders.set(HEADER_REQUEST_ID, finalRequestId);
        initialHeaders.addIfAbsent("X-Content-Type-Options", "nosniff");
        initialHeaders.addIfAbsent("X-Frame-Options", "DENY");
        initialHeaders.addIfAbsent("Referrer-Policy", "strict-origin-when-cross-origin");

        response.beforeCommit(() -> {
            HttpHeaders headers = response.getHeaders();
            headers.set(HEADER_REQUEST_ID, finalRequestId);
            headers.addIfAbsent("X-Content-Type-Options", "nosniff");
            headers.addIfAbsent("X-Frame-Options", "DENY");
            headers.addIfAbsent("Referrer-Policy", "strict-origin-when-cross-origin");
            return Mono.empty();
        });

        return next.apply(exchange.mutate().request(mutatedRequest).build())
                .doFinally(signalType -> {
                    String path = request.getPath().value();
                    if (path.startsWith("/actuator/health")) {
                        return;
                    }
                    long durationMs = (System.nanoTime() - startTimeNs) / 1_000_000;
                    int status = response.getStatusCode() != null ? response.getStatusCode().value() : 500;
                    Object userId = exchange.getAttributeOrDefault(JwtAuthenticationFilter.ATTR_USER_ID, "anonymous");
                    String clientIp = RateLimiterConfig.extractClientIp(request);

                    log.info("ACCESS_LOG method={} path={} status={} durationMs={}ms user={} ip={} requestId={}",
                            request.getMethod(), path, status, durationMs, userId, clientIp, finalRequestId);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
