package vn.edu.fpt.sba.music4.configurations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import vn.edu.fpt.sba.music4.services.IRecommendationService;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchIndexInitializer implements ApplicationRunner {

    private final RestTemplate restTemplate;
    private final IRecommendationService recommendationService;

    @Value("${elasticsearch.url}")
    private String esBaseUrl;

    @Value("${elasticsearch.index.name}")
    private String indexName;

    private static final int VECTOR_DIMS = 384;

    @Override
    public void run(ApplicationArguments args) {
        String indexUrl = esBaseUrl.trim().replaceAll("/$", "") + "/" + indexName;

        try {
            // 1. Kiểm tra index đã tồn tại chưa (HEAD request)
            restTemplate.headForHeaders(indexUrl);
            log.info("[ES-INIT] Index '{}' đã tồn tại, bỏ qua bước tạo mới.", indexName);
        } catch (HttpClientErrorException.NotFound e) {
            // 2. Nếu chưa có (404) -> tạo mới
            log.warn("[ES-INIT] Index '{}' chưa tồn tại. Đang tạo mới...", indexName);
            createIndex(indexUrl);
            log.info("[ES-INIT] Index mới rỗng -> Kích hoạt đồng bộ dữ liệu ngay lập tức...");
            recommendationService.syncTracksToElasticsearch();
        } catch (Exception e) {
            log.error("[ES-INIT] Không thể kiểm tra index '{}': {}", indexName, e.getMessage(), e);
        }
    }

    private void createIndex(String indexUrl) {
        try {
            Map<String, Object> trackId = new HashMap<>();
            trackId.put("type", "integer");

            Map<String, Object> vectorData = new HashMap<>();
            vectorData.put("type", "dense_vector");
            vectorData.put("dims", VECTOR_DIMS);
            vectorData.put("index", true);
            vectorData.put("similarity", "cosine");

            Map<String, Object> properties = new HashMap<>();
            properties.put("track_id", trackId);
            properties.put("vector_data", vectorData);

            Map<String, Object> mappings = new HashMap<>();
            mappings.put("properties", properties);

            Map<String, Object> body = new HashMap<>();
            body.put("mappings", mappings);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.exchange(indexUrl, HttpMethod.PUT, entity, Map.class);
            log.info("[ES-INIT] Tạo index '{}' thành công với mapping dense_vector({} dims).", indexName, VECTOR_DIMS);

        } catch (Exception e) {
            log.error("[ES-INIT] Lỗi khi tạo index '{}': {}", indexName, e.getMessage(), e);
        }
    }
}