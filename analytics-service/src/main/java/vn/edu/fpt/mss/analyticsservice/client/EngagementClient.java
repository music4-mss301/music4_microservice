package vn.edu.fpt.mss.analyticsservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.mss.analyticsservice.dto.client.ArtistEngagementStats;
import vn.edu.fpt.mss.analyticsservice.dto.client.UserSignals;

import java.util.List;

@FeignClient(name = "engagement-service", url = "${services.engagement-service.url:}",
        fallbackFactory = EngagementClientFallbackFactory.class)
public interface EngagementClient {

    @GetMapping("/internal/users/{userId}/signals")
    UserSignals signals(@PathVariable("userId") Integer userId);

    @GetMapping("/internal/artists/{artistId}/stats")
    ArtistEngagementStats artistStats(@PathVariable("artistId") Integer artistId,
                                      @RequestParam("trackIds") List<Integer> trackIds);
}
