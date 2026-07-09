package vn.edu.fpt.sba.music4.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.request.RatingRequest;
import vn.edu.fpt.sba.music4.services.IRatingService;

import java.util.Map;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RatingController {

    private final IRatingService ratingService;

    @PostMapping
    public ResponseEntity<?> saveRating(@Valid @RequestBody RatingRequest request) {
        ratingService.saveRating(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đánh giá bài hát thành công!"
        ));
    }
    @GetMapping("/track/{trackId}/user/{userId}")
    public ResponseEntity<?> getUserRating(
            @PathVariable Integer trackId,
            @PathVariable Integer userId) {
        int score = ratingService.getRatingByUserAndTrack(userId, trackId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "rating", score
        ));
    }
}