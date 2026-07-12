package vn.edu.fpt.sba.music4.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import vn.edu.fpt.sba.music4.services.IRecommendationService;
import vn.edu.fpt.sba.music4.services.ITrackingService;

@Component
@RequiredArgsConstructor
@Slf4j
public class SystemJobScheduler {

    private final IRecommendationService recommendationService;
    private final ITrackingService trackingService;
    private final RestTemplate restTemplate;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    @Value("${elasticsearch.url}")
    private String elasticsearchUrl;

    @Scheduled(cron = "0 0 2 * * ?")
//    @Scheduled(fixedRate = 60000)
    public void scheduleElasticsearchSync() {
        log.info("[CRONJOB] Bắt đầu tiến trình tự động đồng bộ nhạc lên Elasticsearch lúc rạng sáng...");
        recommendationService.syncTracksToElasticsearch();
    }

    @Scheduled(cron = "0 0 3 * * SUN")
//    @Scheduled(fixedRate = 60000)
    public void scheduleHistoryCleanup() {
        log.info("[CRONJOB] Bắt đầu dọn dẹp Database lịch sử rác...");
        trackingService.cleanupHistory();
    }

    @Scheduled(fixedRate = 300000) // 5 phút check 1 lần
    public void healthCheckMicroservices() {
        log.info("[HEALTH CHECK] Đang kiểm tra kết nối tới các Microservices...");

        // Kiểm tra Python AI
        try {
            ResponseEntity<String> pythonHealth = restTemplate.getForEntity(aiServiceUrl, String.class);
            if (pythonHealth.getStatusCode().is2xxSuccessful()) {
                log.info("[OK] Python AI Microservice đang hoạt động tốt.");
            }
        } catch (Exception e) {
            log.error("[CẢNH BÁO] Python AI Microservice KHÔNG PHẢN HỒI. Vui lòng kiểm tra lại server (Port 8000).");
        }

        // Kiểm tra Elasticsearch
        try {
            ResponseEntity<String> esHealth = restTemplate.getForEntity(elasticsearchUrl, String.class);
            if (esHealth.getStatusCode().is2xxSuccessful()) {
                log.info("[OK] Elasticsearch Database đang hoạt động tốt.");
            }
        } catch (Exception e) {
            log.error("[CẢNH BÁO] Elasticsearch KHÔNG PHẢN HỒI. Vui lòng kiểm tra lại Docker/Service (Port 9200).");
        }
    }
}