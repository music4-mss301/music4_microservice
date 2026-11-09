package vn.edu.fpt.mss.apigateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.TimeoutException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Xử lý tập trung toàn bộ ngoại lệ chưa bắt được ở tầng Gateway / Netty / LoadBalancer,
 * đảm bảo Frontend luôn nhận được JSON đồng nhất thay vì trang lỗi mặc định.
 */
@Slf4j
@Order(-1)
@Component
@RequiredArgsConstructor
public class GlobalGatewayExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = resolveHttpStatus(ex);
        String message = resolveUserMessage(status);
        String path = exchange.getRequest().getPath().value();

        log.error("Gateway Error [{} {}] -> status={}, cause={}: {}",
                exchange.getRequest().getMethod(), path, status.value(),
                ex.getClass().getSimpleName(), ex.getMessage());

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);

        String requestId = response.getHeaders().getFirst("X-Request-Id");
        if (requestId != null) {
            body.put("requestId", requestId);
        }

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception writeEx) {
            bytes = message.getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private HttpStatus resolveHttpStatus(Throwable ex) {
        if (ex instanceof NotFoundException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (ex instanceof TimeoutException || ex instanceof java.util.concurrent.TimeoutException) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        if (ex instanceof ConnectException) {
            return HttpStatus.BAD_GATEWAY;
        }
        if (ex instanceof ResponseStatusException rse) {
            HttpStatusCode code = rse.getStatusCode();
            HttpStatus resolved = HttpStatus.resolve(code.value());
            return resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveUserMessage(HttpStatus status) {
        return switch (status) {
            case SERVICE_UNAVAILABLE -> "Dịch vụ đang khởi động hoặc tạm thời bảo trì, vui lòng thử lại sau giây lát";
            case GATEWAY_TIMEOUT -> "Hệ thống xử lý quá thời gian quy định (Gateway Timeout), vui lòng thử lại";
            case BAD_GATEWAY -> "Không thể kết nối tới dịch vụ xử lý phía sau";
            case TOO_MANY_REQUESTS -> "Bạn đã gửi quá nhiều yêu cầu trong thời gian ngắn, vui lòng chậm lại";
            case PAYLOAD_TOO_LARGE -> "Kích thước dữ liệu gửi lên vượt quá giới hạn cho phép";
            case NOT_FOUND -> "Không tìm thấy đường dẫn API yêu cầu";
            default -> "Đã xảy ra lỗi tại cổng giao tiếp hệ thống";
        };
    }
}
