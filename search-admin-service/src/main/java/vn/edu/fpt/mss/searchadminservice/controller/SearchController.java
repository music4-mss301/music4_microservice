package vn.edu.fpt.mss.searchadminservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.mss.searchadminservice.dto.response.SearchResponse;
import vn.edu.fpt.mss.searchadminservice.service.SearchService;

import java.util.List;

@Tag(name = "Search")
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public SearchResponse search(@RequestParam String q,
                                 @RequestParam(defaultValue = "all") String type,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        return searchService.search(q, type, page, size);
    }

    @GetMapping("/suggest")
    public List<String> suggest(@RequestParam String q) {
        return searchService.suggest(q);
    }
}
