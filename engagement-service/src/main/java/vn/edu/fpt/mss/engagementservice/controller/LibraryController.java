package vn.edu.fpt.mss.engagementservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.mss.engagementservice.dto.client.TrackSummary;
import vn.edu.fpt.mss.engagementservice.dto.request.RatingRequest;
import vn.edu.fpt.mss.engagementservice.dto.response.*;
import vn.edu.fpt.mss.engagementservice.exception.ForbiddenException;
import vn.edu.fpt.mss.engagementservice.security.SecurityUtils;
import vn.edu.fpt.mss.engagementservice.service.LibraryService;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Favorites, follows, ratings: giữ đúng path FE đang gọi. userId luôn lấy từ token. */
@Tag(name = "Library (favorite / follow / rating)")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;

    // ------------------------------------------------ favorites
    @PostMapping("/favorites/toggle/{trackId}")
    public FavoriteToggleResponse toggleFavorite(@PathVariable Integer trackId) {
        return libraryService.toggleFavorite(trackId);
    }

    @GetMapping("/favorites/me")
    public List<TrackSummary> myFavorites() {
        return libraryService.myFavorites();
    }

    @GetMapping("/favorites/check/{trackId}")
    public Map<String, Boolean> isFavorite(@PathVariable Integer trackId) {
        return Map.of("favorited", libraryService.isFavorite(trackId));
    }

    @GetMapping("/favorites/track/{trackId}/count")
    public Map<String, Long> favoriteCount(@PathVariable Integer trackId) {
        return Map.of("count", libraryService.countFavorites(trackId));
    }

    // ------------------------------------------------ follows
    /** FE cũ gửi kèm userId trên query: bị bỏ qua, chỉ dùng user trong token. */
    @PostMapping("/follows/toggle")
    public FollowToggleResponse toggleFollow(@RequestParam Integer artistId) {
        return libraryService.toggleFollow(artistId);
    }

    @GetMapping("/follows/user/{userId}")
    public List<FollowResponse> following(@PathVariable Integer userId) {
        return libraryService.following(userId);
    }

    @GetMapping("/follows/check/{artistId}")
    public Map<String, Boolean> isFollowing(@PathVariable Integer artistId) {
        return Map.of("following", libraryService.isFollowing(artistId));
    }

    @GetMapping("/follows/artist/{artistId}/count")
    public Map<String, Long> followerCount(@PathVariable Integer artistId) {
        return Map.of("followers", libraryService.countFollowers(artistId));
    }

    // ------------------------------------------------ ratings
    @PostMapping("/ratings")
    public ResponseEntity<RatingResponse> rate(@Valid @RequestBody RatingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libraryService.rate(request));
    }

    @GetMapping("/ratings/average/{trackId}")
    public RatingSummaryResponse average(@PathVariable Integer trackId) {
        return libraryService.ratingSummary(trackId);
    }

    @GetMapping("/ratings/track/{trackId}/me")
    public RatingResponse myRating(@PathVariable Integer trackId) {
        return libraryService.myRating(trackId);
    }

    /** Path cũ của FE: chỉ được xem rating của chính mình. */
    @GetMapping("/ratings/track/{trackId}/user/{userId}")
    public RatingResponse ratingOf(@PathVariable Integer trackId, @PathVariable Integer userId) {
        if (!Objects.equals(SecurityUtils.requireUser().id(), userId)) {
            throw new ForbiddenException("Chỉ xem được đánh giá của chính mình");
        }
        return libraryService.myRating(trackId);
    }

    @DeleteMapping("/ratings/track/{trackId}")
    public ResponseEntity<Void> deleteRating(@PathVariable Integer trackId) {
        libraryService.deleteMyRating(trackId);
        return ResponseEntity.noContent().build();
    }
}
