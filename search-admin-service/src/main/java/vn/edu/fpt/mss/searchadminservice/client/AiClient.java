package vn.edu.fpt.mss.searchadminservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import vn.edu.fpt.mss.searchadminservice.config.SearchProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gọi AI-service (Python, stateless) để biến text thành vector. Bọc circuit breaker "ai":
 * AI chết hoặc chưa cấu hình thì trả null, document được đánh dấu needsEmbedding để embed bù sau.
 */
@Slf4j
@Component
public class AiClient {

    private final RestClient rest;
    private final SearchProperties props;
    private final CircuitBreakerFactory<?, ?> cbFactory;

    public AiClient(SearchProperties props, RestClient.Builder builder, CircuitBreakerFactory<?, ?> cbFactory) {
        this.props = props;
        this.cbFactory = cbFactory;
        this.rest = StringUtils.hasText(props.aiUrl()) ? builder.baseUrl(props.aiUrl()).build() : null;
    }

    public List<Float> embed(String text) {
        if (rest == null || !StringUtils.hasText(text)) {
            return null;
        }
        return cbFactory.create("ai").run(() -> {
            JsonNode res = rest.post().uri("/api/ai/embed").header("X-API-Key", props.aiApiKey())
                    .body(Map.of("text", text)).retrieve().body(JsonNode.class);
            List<Float> vector = new ArrayList<>();
            if (res != null && res.get("vector") != null) {
                res.get("vector").forEach(v -> vector.add(v.floatValue()));
            }
            return vector.size() == props.vectorDims() ? vector : null;
        }, ex -> {
            log.warn("AI-service lỗi, tạm index không vector: {}", ex.getMessage());
            return null;
        });
    }
}
