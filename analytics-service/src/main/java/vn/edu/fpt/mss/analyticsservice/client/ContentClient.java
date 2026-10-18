package vn.edu.fpt.mss.analyticsservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.mss.analyticsservice.dto.client.ArtistStats;
import vn.edu.fpt.mss.analyticsservice.dto.client.TrackSummary;

import java.util.List;

@FeignClient(name = "content-service", url = "${services.content-service.url:}",
        fallbackFactory = ContentClientFallbackFactory.class)
public interface ContentClient {

    @GetMapping("/internal/tracks")
    List<TrackSummary> getTracks(@RequestParam("ids") List<Integer> ids);

    @GetMapping("/internal/tracks/trending")
    List<TrackSummary> trending(@RequestParam("limit") int limit);

    @GetMapping("/internal/artists/{id}/stats")
    ArtistStats artistStats(@PathVariable("id") Integer artistId);
}
