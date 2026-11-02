package vn.edu.fpt.mss.searchadminservice.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.mss.searchadminservice.dto.request.RecommendQuery;
import vn.edu.fpt.mss.searchadminservice.dto.response.SuggestionResponse;
import vn.edu.fpt.mss.searchadminservice.service.SearchService;

import java.util.List;

@Hidden
@RestController
@RequestMapping("/internal/search")
@RequiredArgsConstructor
public class InternalSearchController {

    private final SearchService searchService;

    @PostMapping("/recommend")
    public List<SuggestionResponse> recommend(@Valid @RequestBody RecommendQuery query) {
        return searchService.recommend(query);
    }
}
