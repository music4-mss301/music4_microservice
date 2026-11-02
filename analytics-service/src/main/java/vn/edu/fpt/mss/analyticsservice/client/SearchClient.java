package vn.edu.fpt.mss.analyticsservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import vn.edu.fpt.mss.analyticsservice.dto.client.Suggestion;
import vn.edu.fpt.mss.analyticsservice.dto.request.RecommendQuery;

import java.util.List;

@FeignClient(name = "search-admin-service", url = "${services.search-admin-service.url:}",
        fallbackFactory = SearchClientFallbackFactory.class)
public interface SearchClient {

    @PostMapping("/internal/search/recommend")
    List<Suggestion> recommend(@RequestBody RecommendQuery query);
}
