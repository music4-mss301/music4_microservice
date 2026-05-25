package vn.edu.fpt.sba.music4.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.request.RatingRequest;
import vn.edu.fpt.sba.music4.dtos.response.ArtistOverviewResponse;
import vn.edu.fpt.sba.music4.dtos.response.IChartData;
import vn.edu.fpt.sba.music4.services.IAnalyticsService;
import vn.edu.fpt.sba.music4.services.IRatingService;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AnalyticsController {

    private final IAnalyticsService analyticsService;
    private final IRatingService ratingService;

    @GetMapping("/artist/{artistId}/views")
    public ResponseEntity<List<IChartData>> getArtistDailyViews(@PathVariable Integer artistId) {
        return ResponseEntity.ok(analyticsService.getArtistDailyViews(artistId));
    }

    @PostMapping("/rating")
    public ResponseEntity<String> rateTrack(@Valid @RequestBody RatingRequest request) {
        ratingService.saveRating(request);
        return ResponseEntity.ok("Thank you for rating the song!");
    }

    @GetMapping("/artist/{artistId}/overview")
    public ResponseEntity<ArtistOverviewResponse> getArtistOverview(@PathVariable Integer artistId) {
        return ResponseEntity.ok(analyticsService.getArtistOverview(artistId));
    }
}
