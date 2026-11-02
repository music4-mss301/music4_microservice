package vn.edu.fpt.mss.searchadminservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.edu.fpt.mss.searchadminservice.dto.client.PageResult;
import vn.edu.fpt.mss.searchadminservice.dto.client.StatsDtos;
import vn.edu.fpt.mss.searchadminservice.dto.client.TrackDoc;
import vn.edu.fpt.mss.searchadminservice.dto.client.TrackSummary;

@FeignClient(name = "content-service", url = "${services.content-service.url:}",
        fallbackFactory = AdminClientsFallbacks.Content.class)
public interface ContentClient {

    @GetMapping("/internal/tracks/export")
    PageResult<TrackDoc> export(@RequestParam("page") int page, @RequestParam("size") int size);

    /** Fallback khi ES chết: tìm bằng LIKE trên DB của content. */
    @GetMapping("/internal/tracks/search")
    PageResult<TrackSummary> search(@RequestParam("keyword") String keyword, @RequestParam("page") int page,
                                    @RequestParam("size") int size);

    @GetMapping("/internal/stats")
    StatsDtos.ContentStats stats();
}
