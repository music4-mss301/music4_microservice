package vn.edu.fpt.mss.engagementservice.service;

import vn.edu.fpt.mss.engagementservice.dto.client.TrackSummary;
import vn.edu.fpt.mss.engagementservice.dto.request.RatingRequest;
import vn.edu.fpt.mss.engagementservice.dto.response.*;

import java.util.List;

/** Tương tác của người nghe với bài hát và nghệ sĩ: favorite, follow, rating. */
public interface LibraryService {
    FavoriteToggleResponse toggleFavorite(Integer trackId);

    boolean isFavorite(Integer trackId);

    List<TrackSummary> myFavorites();

    long countFavorites(Integer trackId);

    FollowToggleResponse toggleFollow(Integer artistId);

    boolean isFollowing(Integer artistId);

    List<FollowResponse> following(Integer userId);

    long countFollowers(Integer artistId);

    RatingResponse rate(RatingRequest request);

    RatingSummaryResponse ratingSummary(Integer trackId);

    RatingResponse myRating(Integer trackId);

    void deleteMyRating(Integer trackId);
}
