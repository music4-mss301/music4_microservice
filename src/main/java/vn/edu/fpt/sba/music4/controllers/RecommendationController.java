package vn.edu.fpt.sba.music4.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.response.TrackSuggestResponse;
import vn.edu.fpt.sba.music4.services.IRecommendationService;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@CrossOrigin("*")
public class RecommendationController {

    private final IRecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<List<TrackSuggestResponse>> getRecommendations(
            @RequestParam(required = false) Integer userId) {

        List<TrackSuggestResponse> suggestions = recommendationService.getRecommendations(userId);
        return ResponseEntity.ok(suggestions);
    }
}