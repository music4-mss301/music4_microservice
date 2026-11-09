package vn.edu.fpt.mss.apigateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.TimeoutException;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import vn.edu.fpt.mss.apigateway.config.GatewaySecurityProperties;
import vn.edu.fpt.mss.apigateway.config.RateLimiterConfig;
import vn.edu.fpt.mss.apigateway.controller.FallbackController;
import vn.edu.fpt.mss.apigateway.exception.GlobalGatewayExceptionHandler;
import vn.edu.fpt.mss.apigateway.filter.JwtAuthenticationFilter;
import vn.edu.fpt.mss.apigateway.filter.ObservabilityAndSecurityHeadersFilter;

import javax.crypto.SecretKey;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class ApiGatewayFeaturesTest {

    private static final String RAW_SECRET = "music4-super-secret-jwt-key-must-be-at-least-32-bytes-long";
    private static final String BASE64_SECRET = Base64.getEncoder()
            .encodeToString(RAW_SECRET.getBytes(StandardCharsets.UTF_8));
    private static final String GATEWAY_SECRET = "test-gateway-secret";

    private SecretKey secretKey;
    private ObjectMapper objectMapper;
    private GatewaySecurityProperties securityProps;
    private ReactiveStringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(BASE64_SECRET));
        objectMapper = new ObjectMapper();
        redisTemplate = Mockito.mock(ReactiveStringRedisTemplate.class);
        securityProps = new GatewaySecurityProperties(
                BASE64_SECRET,
                GATEWAY_SECRET,
                List.of("POST:/api/auth/**", "GET:/api/tracks/**"),
                List.of("/api/tracks/stream/*", "/api/media/stream", "/api/notifications/subscribe/*"),
                true
        );
    }

    private String generateToken(Integer id, String username, String role, Integer artistId, String jti, long ttlMs) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(username)
                .claim("id", id)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs));
        if (artistId != null) {
            builder.claim("artistId", artistId);
        }
        if (jti != null) {
            builder.id(jti);
        }
        return builder.signWith(secretKey).compact();
    }

    @Test
    @DisplayName("1. Chặn /internal/** từ bên ngoài và gắn đầy đủ OWASP Security Headers + X-Request-Id")
    void shouldBlockInternalEndpointsAndAttachSecurityHeaders() throws Exception {
        ObservabilityAndSecurityHeadersFilter obsFilter = new ObservabilityAndSecurityHeadersFilter(null);
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(securityProps, objectMapper, null);

        MockServerHttpRequest request = MockServerHttpRequest.get("/internal/users/1").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = ex -> jwtFilter.filter(ex, ex2 -> Mono.empty());
        obsFilter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        HttpHeaders respHeaders = exchange.getResponse().getHeaders();
        assertThat(respHeaders.getFirst("X-Request-Id")).isNotBlank();
        assertThat(respHeaders.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(respHeaders.getFirst("X-Frame-Options")).isEqualTo("DENY");
        assertThat(respHeaders.getFirst("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");

        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(body).contains("\"status\":404", "\"requestId\"");
    }

    @Test
    @DisplayName("2. Xóa header giả mạo (Anti-Spoofing) và trích xuất đầy đủ X-User-Id, X-User-Role, X-Artist-Id")
    void shouldStripSpoofedHeadersAndPopulateIdentityHeadersIncludingArtistId() {
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(securityProps, objectMapper, redisTemplate);

        String validArtistToken = generateToken(15, "sontungmtp", "ROLE_ARTIST", 77, "jti-1", 60_000);
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/tracks")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validArtistToken)
                .header("X-User-Id", "9999")
                .header("X-User-Role", "ADMIN")
                .header("X-Internal-Token", "hacked")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        jwtFilter.filter(exchange, ex -> {
            forwarded.set(ex);
            return Mono.empty();
        }).block();

        assertThat(forwarded.get()).isNotNull();
        HttpHeaders downstreamHeaders = forwarded.get().getRequest().getHeaders();
        assertThat(downstreamHeaders.getFirst("X-User-Id")).isEqualTo("15");
        assertThat(downstreamHeaders.getFirst("X-Username")).isEqualTo("sontungmtp");
        assertThat(downstreamHeaders.getFirst("X-User-Role")).isEqualTo("ARTIST");
        assertThat(downstreamHeaders.getFirst("X-Artist-Id")).isEqualTo("77");
        assertThat(downstreamHeaders.getFirst("X-Gateway-Secret")).isEqualTo(GATEWAY_SECRET);
        assertThat(downstreamHeaders.getFirst("X-Internal-Token")).isNull();
        assertThat( (String) forwarded.get().getAttribute(JwtAuthenticationFilter.ATTR_USER_ID)).isEqualTo("15");
    }

    @Test
    @DisplayName("3. Public Path: khi gửi kèm token đã hết hạn vẫn tự động hạ cấp về Anonymous thay vì lỗi 401")
    void shouldFallbackToAnonymousOnPublicPathWhenTokenIsExpired() {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(securityProps, objectMapper, null);

        String expiredToken = generateToken(10, "listener1", "USER", null, null, -10_000);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/tracks/100")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .header("X-User-Id", "999")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        jwtFilter.filter(exchange, ex -> {
            forwarded.set(ex);
            return Mono.empty();
        }).block();

        assertThat(forwarded.get()).isNotNull();
        HttpHeaders headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isNull();
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isNull();
        assertThat(headers.getFirst("X-Gateway-Secret")).isEqualTo(GATEWAY_SECRET);
    }

    @Test
    @DisplayName("4. Bảo vệ /actuator/**: mở /actuator/health nhưng chặn USER thường ở /actuator/gateway/routes")
    void shouldProtectSensitiveActuatorEndpointsForAdminOnly() {
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(securityProps, objectMapper, redisTemplate);

        // 4.1. /actuator/health cho qua không cần token
        MockServerWebExchange healthEx = MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/health").build());
        AtomicReference<Boolean> healthPassed = new AtomicReference<>(false);
        jwtFilter.filter(healthEx, ex -> {
            healthPassed.set(true);
            return Mono.empty();
        }).block();
        assertThat(healthPassed.get()).isTrue();

        // 4.2. /actuator/gateway/routes với token USER thường -> 403 Forbidden
        String userToken = generateToken(2, "user2", "USER", null, null, 60_000);
        MockServerWebExchange userActuatorEx = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/gateway/routes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .build());
        jwtFilter.filter(userActuatorEx, ex -> Mono.empty()).block();
        assertThat(userActuatorEx.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // 4.3. /actuator/gateway/routes với token ADMIN -> Cho phép đi tiếp
        String adminToken = generateToken(1, "admin", "ADMIN", null, null, 60_000);
        MockServerWebExchange adminActuatorEx = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/gateway/routes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .build());
        AtomicReference<Boolean> adminPassed = new AtomicReference<>(false);
        jwtFilter.filter(adminActuatorEx, ex -> {
            adminPassed.set(true);
            return Mono.empty();
        }).block();
        assertThat(adminPassed.get()).isTrue();
    }

    @Test
    @DisplayName("5. Redis Blacklist: Chặn ngay lập tức khi User bị khóa và tự động Fail-Open khi Redis lỗi")
    void shouldRejectBannedUserInRedisAndFailOpenWhenRedisIsDown() {
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(securityProps, objectMapper, redisTemplate);
        String validToken = generateToken(25, "bannedUser", "USER", null, "jti-25", 60_000);

        // 5.1. Khi Redis báo auth:banned:user:25 = true -> chặn 401
        when(redisTemplate.hasKey("auth:banned:user:25")).thenReturn(Mono.just(true));
        when(redisTemplate.hasKey("auth:blacklist:jti:jti-25")).thenReturn(Mono.just(false));

        MockServerWebExchange bannedEx = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .build());
        jwtFilter.filter(bannedEx, ex -> Mono.empty()).block();
        assertThat(bannedEx.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 5.2. Khi Redis gặp sự cố kết nối -> Fail-open (vẫn cho phép request hợp lệ đi qua)
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.error(new RuntimeException("Redis connection refused")));

        MockServerWebExchange failOpenEx = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                        .build());
        AtomicReference<Boolean> passedOnRedisDown = new AtomicReference<>(false);
        jwtFilter.filter(failOpenEx, ex -> {
            passedOnRedisDown.set(true);
            return Mono.empty();
        }).block();
        assertThat(passedOnRedisDown.get()).isTrue();
    }

    @Test
    @DisplayName("6. Hỗ trợ ?token= cho luồng audio stream và giữ nguyên query param cho 302 redirect")
    void shouldExtractQueryTokenOnStreamPathAndPreserveQueryParam() {
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(securityProps, objectMapper, redisTemplate);
        String validToken = generateToken(9, "streamer", "USER", null, null, 60_000);

        MockServerWebExchange streamEx = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/tracks/stream/42?token=" + validToken).build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        jwtFilter.filter(streamEx, ex -> {
            forwarded.set(ex);
            return Mono.empty();
        }).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("9");
        assertThat(forwarded.get().getRequest().getQueryParams().getFirst("token")).isEqualTo(validToken);
    }

    @Test
    @DisplayName("7. RateLimiter KeyResolver & Custom JSON 429 Filter hoạt động chuẩn xác")
    @SuppressWarnings("unchecked")
    void shouldResolveRateLimitKeyByUserOrIp() {
        RateLimiterConfig config = new RateLimiterConfig();
        KeyResolver resolver = config.userOrIpKeyResolver();

        // Khi có user đã xác thực
        MockServerWebExchange authEx = MockServerWebExchange.from(MockServerHttpRequest.get("/api/tracks").build());
        authEx.getAttributes().put(JwtAuthenticationFilter.ATTR_USER_ID, "42");
        assertThat(resolver.resolve(authEx).block()).isEqualTo("user:42");

        // Khi là khách đi qua Reverse Proxy có X-Forwarded-For
        MockServerWebExchange anonEx = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/tracks")
                        .header("X-Forwarded-For", "203.113.152.1, 10.0.0.1")
                        .build());
        assertThat(resolver.resolve(anonEx).block()).isEqualTo("ip:203.113.152.1");

        // Kiểm tra Custom RequestRateLimiterGatewayFilterFactory khi vượt ngưỡng (isAllowed = false) -> trả JSON 429
        RateLimiter<Object> mockLimiter = Mockito.mock(RateLimiter.class);
        when(mockLimiter.isAllowed(anyString(), anyString())).thenReturn(Mono.just(
                new RateLimiter.Response(false, Map.of("X-RateLimit-Remaining", "0"))));

        RequestRateLimiterGatewayFilterFactory factory = config.requestRateLimiterGatewayFilterFactory(
                mockLimiter, resolver, objectMapper);
        RequestRateLimiterGatewayFilterFactory.Config filterConfig = new RequestRateLimiterGatewayFilterFactory.Config();
        filterConfig.setRouteId("user-service");
        GatewayFilter filter = factory.apply(filterConfig);

        MockServerWebExchange limitedEx = MockServerWebExchange.from(MockServerHttpRequest.post("/api/auth/login").build());
        limitedEx.getResponse().getHeaders().set("X-Request-Id", "req-limit-1");
        filter.filter(limitedEx, ex -> Mono.empty()).block();

        assertThat(limitedEx.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(limitedEx.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("1");
        assertThat(limitedEx.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("0");
        String body = limitedEx.getResponse().getBodyAsString().block();
        assertThat(body).contains("\"status\":429", "\"requestId\":\"req-limit-1\"", "Bạn đã gửi quá nhiều yêu cầu");
    }

    @Test
    @DisplayName("8. GlobalGatewayExceptionHandler & FallbackController chuẩn hóa JSON cho 429, 502, 503, 504")
    void shouldFormatGlobalExceptionsAndFallbackConsistently() {
        GlobalGatewayExceptionHandler handler = new GlobalGatewayExceptionHandler(objectMapper);

        // 429 Too Many Requests
        MockServerWebExchange rateLimitEx = MockServerWebExchange.from(MockServerHttpRequest.post("/api/auth/login").build());
        rateLimitEx.getResponse().getHeaders().set("X-Request-Id", "req-429");
        handler.handle(rateLimitEx, new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS)).block();
        assertThat(rateLimitEx.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(rateLimitEx.getResponse().getBodyAsString().block()).contains("\"status\":429", "\"requestId\":\"req-429\"");

        // 504 Gateway Timeout
        MockServerWebExchange timeoutEx = MockServerWebExchange.from(MockServerHttpRequest.get("/api/search").build());
        handler.handle(timeoutEx, new TimeoutException(" Timed out")).block();
        assertThat(timeoutEx.getResponse().getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);

        // 502 Bad Gateway
        MockServerWebExchange connectEx = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users").build());
        handler.handle(connectEx, new ConnectException("Connection refused")).block();
        assertThat(connectEx.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);

        // 503 Service Unavailable (LoadBalancer NotFoundException)
        MockServerWebExchange lbNotFoundEx = MockServerWebExchange.from(MockServerHttpRequest.get("/api/analytics").build());
        handler.handle(lbNotFoundEx, new NotFoundException("Unable to find instance for analytics-service")).block();
        assertThat(lbNotFoundEx.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        // FallbackController
        FallbackController fallbackController = new FallbackController();
        MockServerWebExchange fbEx = MockServerWebExchange.from(MockServerHttpRequest.get("/fallback/user-service").build());
        ResponseEntity<Map<String, Object>> fbResp = fallbackController.fallback("user-service", fbEx);
        assertThat(fbResp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(fbResp.getBody()).containsEntry("service", "user-service");
    }

    @Nested
    @SpringBootTest(
            webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
            properties = {
                    "spring.cloud.config.enabled=false",
                    "spring.cloud.bus.enabled=false",
                    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
                    "eureka.client.enabled=false",
                    "app.gateway.jwt-secret=bXVzaWM0LXN1cGVyLXNlY3JldC1qd3Qta2V5LW11c3QtYmUtYXQtbGVhc3QtMzItYnl0ZXMtbG9uZw==",
                    "app.gateway.gateway-secret=test-gateway-secret"
            }
    )
    @AutoConfigureWebTestClient
    class FullSpringContextIntegrationTest {

        @Autowired
        private WebTestClient webTestClient;

        @Autowired
        private RouteLocator routeLocator;

        @Test
        @DisplayName("9. Full Spring Context: Nạp đầy đủ 9 routes, actuator/health, fallback và chặn /internal/**")
        void shouldBootFullGatewayContextAndServeEndpoints() {
            var routes = routeLocator.getRoutes().collectList().block();
            assertThat(routes).isNotNull();
            assertThat(routes).extracting("id").contains(
                    "engagement-sse", "artist-followers", "recommendations-sync",
                    "user-service", "content-service", "engagement-service",
                    "analytics-service", "media-service", "search-admin-service"
            );

            // Healthcheck public
            webTestClient.get().uri("/actuator/health")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().exists("X-Request-Id")
                    .expectHeader().valueEquals("X-Content-Type-Options", "nosniff");

            // Chặn /actuator/gateway/routes khi chưa đăng nhập
            webTestClient.get().uri("/actuator/gateway/routes")
                    .exchange()
                    .expectStatus().isUnauthorized();

            // Chặn /internal/**
            webTestClient.get().uri("/internal/secret")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(404)
                    .jsonPath("$.requestId").isNotEmpty();

            // Fallback endpoint trả 503
            webTestClient.get().uri("/fallback/content-service")
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                    .expectBody()
                    .jsonPath("$.service").isEqualTo("content-service");
        }
    }
}
