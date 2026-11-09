package vn.edu.fpt.mss.apigateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Circuit breaker của gateway mở hoặc service timeout: trả 503 JSON thay vì treo request. */
@Slf4j
@RestController
public class FallbackController {

    @RequestMapping("/fallback/{service}")
    public ResponseEntity<Map<String, Object>> fallback(
            @PathVariable String service,
            ServerWebExchange exchange) {

        Throwable cause = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
        if (cause != null) {
            log.warn("CircuitBreaker fallback kích hoạt cho service={} nguyên nhân={}: {}",
                    service, cause.getClass().getSimpleName(), cause.getMessage());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", "Service Unavailable");
        body.put("message", service + " đang tạm thời không phản hồi, vui lòng thử lại sau");
        body.put("service", service);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
