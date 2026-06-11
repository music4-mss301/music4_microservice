package vn.edu.fpt.sba.music4.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.sba.music4.dtos.response.SearchResultResponseDTO;
import vn.edu.fpt.sba.music4.services.ISearchService;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final ISearchService searchService;
    public SearchController(ISearchService searchService) {
        this.searchService = searchService;
    }
    @GetMapping("")
    public ResponseEntity<SearchResultResponseDTO> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (q == null || q.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(searchService.search(q, type, page, size));
    }
}
