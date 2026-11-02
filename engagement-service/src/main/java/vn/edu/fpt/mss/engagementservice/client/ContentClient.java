package vn.edu.fpt.mss.engagementservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.mss.engagementservice.dto.client.ArtistSummary;
import vn.edu.fpt.mss.engagementservice.dto.client.TrackSummary;

import java.util.List;

@FeignClient(name = "content-service", url = "${services.content-service.url:}",
        fallbackFactory = ContentClientFallbackFactory.class)
public interface ContentClient {

    /** Batch: 1 call cho cả danh sách, không gọi từng bài. */
    @GetMapping("/internal/tracks")
    List<TrackSummary> getTracks(@RequestParam("ids") List<Integer> ids);

    @GetMapping("/internal/artists")
    List<ArtistSummary> getArtists(@RequestParam("ids") List<Integer> ids);

    @GetMapping("/internal/artists/{id}")
    ArtistSummary getArtist(@PathVariable("id") Integer id);
}
