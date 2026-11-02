package vn.edu.fpt.mss.contentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import vn.edu.fpt.mss.contentservice.dto.client.ArtistEngagementQuery;
import vn.edu.fpt.mss.contentservice.dto.client.ArtistEngagementResult;

import java.util.List;

@FeignClient(name = "engagement-service", url = "${services.engagement-service.url:}",
        fallbackFactory = EngagementClientFallbackFactory.class)
public interface EngagementClient {

    /** Một lần gọi cho cả danh sách nghệ sĩ (không gọi từng người). */
    @PostMapping("/internal/artists/engagement")
    List<ArtistEngagementResult> artistEngagement(@RequestBody List<ArtistEngagementQuery> queries);
}
