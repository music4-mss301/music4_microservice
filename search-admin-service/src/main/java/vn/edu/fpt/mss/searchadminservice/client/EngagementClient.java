package vn.edu.fpt.mss.searchadminservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import vn.edu.fpt.mss.searchadminservice.dto.client.StatsDtos;

@FeignClient(name = "engagement-service", url = "${services.engagement-service.url:}",
        fallbackFactory = AdminClientsFallbacks.Engagement.class)
public interface EngagementClient {

    @GetMapping("/internal/stats")
    StatsDtos.EngagementStats stats();
}
