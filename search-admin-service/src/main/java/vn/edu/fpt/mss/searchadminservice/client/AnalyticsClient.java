package vn.edu.fpt.mss.searchadminservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.mss.searchadminservice.dto.client.StatsDtos;

import java.util.Map;

@FeignClient(name = "analytics-service", url = "${services.analytics-service.url:}",
        fallbackFactory = AdminClientsFallbacks.Analytics.class)
public interface AnalyticsClient {

    @GetMapping("/internal/history/stats")
    StatsDtos.AnalyticsStats stats();

    @DeleteMapping("/internal/history/cleanup")
    Map<String, Integer> cleanup(@RequestParam("days") int days);
}
