package vn.edu.fpt.mss.engagementservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.mss.engagementservice.client.ContentClient;
import vn.edu.fpt.mss.engagementservice.dto.client.ArtistSummary;
import vn.edu.fpt.mss.engagementservice.dto.client.TrackSummary;
import vn.edu.fpt.mss.engagementservice.dto.request.RatingRequest;
import vn.edu.fpt.mss.engagementservice.dto.response.*;
import vn.edu.fpt.mss.engagementservice.entity.Favorite;
import vn.edu.fpt.mss.engagementservice.entity.Follow;
import vn.edu.fpt.mss.engagementservice.entity.Rating;
import vn.edu.fpt.mss.engagementservice.exception.BusinessException;
import vn.edu.fpt.mss.engagementservice.exception.ResourceNotFoundException;
import vn.edu.fpt.mss.engagementservice.repository.FavoriteRepository;
import vn.edu.fpt.mss.engagementservice.repository.FollowRepository;
import vn.edu.fpt.mss.engagementservice.repository.RatingRepository;
import vn.edu.fpt.mss.engagementservice.security.SecurityUtils;
import vn.edu.fpt.mss.engagementservice.service.LibraryService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LibraryServiceImpl implements LibraryService {

    private final FavoriteRepository favoriteRepository;
    private final FollowRepository followRepository;
    private final RatingRepository ratingRepository;
    private final ContentClient contentClient;

    // -------------------------------------------------- favorite

    /** Rule: mỗi user chỉ favorite 1 lần mỗi bài (unique user_id+track_id), gọi lại = bỏ thích. */
    @Override
    @Transactional
    public FavoriteToggleResponse toggleFavorite(Integer trackId) {
        Integer userId = SecurityUtils.requireUser().id();
        var existing = favoriteRepository.findByUserIdAndTrackId(userId, trackId);
        boolean favorited;
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            favorited = false;
        } else {
            requireTrackExists(trackId);
            favoriteRepository.save(Favorite.builder().userId(userId).trackId(trackId).build());
            favorited = true;
        }
        favoriteRepository.flush();
        return new FavoriteToggleResponse(trackId, favorited, favoriteRepository.countByTrackId(trackId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorite(Integer trackId) {
        return favoriteRepository.existsByUserIdAndTrackId(SecurityUtils.requireUser().id(), trackId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackSummary> myFavorites() {
        List<Integer> ids = favoriteRepository.findByUserIdOrderByLikedAtDesc(SecurityUtils.requireUser().id())
                .stream().map(Favorite::getTrackId).toList();
        return ids.isEmpty() ? List.of() : contentClient.getTracks(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public long countFavorites(Integer trackId) {
        return favoriteRepository.countByTrackId(trackId);
    }

    // -------------------------------------------------- follow

    /** Rule: không tự follow hồ sơ nghệ sĩ của chính mình; nghệ sĩ phải tồn tại. */
    @Override
    @Transactional
    public FollowToggleResponse toggleFollow(Integer artistId) {
        Integer userId = SecurityUtils.requireUser().id();
        var existing = followRepository.findByUserIdAndArtistId(userId, artistId);
        boolean following;
        if (existing.isPresent()) {
            followRepository.delete(existing.get());
            following = false;
        } else {
            ArtistSummary artist = contentClient.getArtist(artistId);
            if (Objects.equals(artist.userId(), userId)) {
                throw new BusinessException("Không thể tự theo dõi chính mình");
            }
            followRepository.save(Follow.builder().userId(userId).artistId(artistId).build());
            following = true;
        }
        followRepository.flush();
        return new FollowToggleResponse(artistId, following, followRepository.countByArtistId(artistId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(Integer artistId) {
        return followRepository.existsByUserIdAndArtistId(SecurityUtils.requireUser().id(), artistId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowResponse> following(Integer userId) {
        List<Follow> follows = followRepository.findByUserIdOrderByFollowedAtDesc(userId);
        if (follows.isEmpty()) {
            return List.of();
        }
        Map<Integer, ArtistSummary> artists = contentClient.getArtists(follows.stream().map(Follow::getArtistId).toList())
                .stream().collect(Collectors.toMap(ArtistSummary::id, Function.identity(), (a, b) -> a));
        return follows.stream().map(f -> {
            ArtistSummary a = artists.get(f.getArtistId());
            return new FollowResponse(f.getArtistId(), a == null ? null : a.name(), a == null ? null : a.img(),
                    f.getFollowedAt());
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countFollowers(Integer artistId) {
        return followRepository.countByArtistId(artistId);
    }

    // -------------------------------------------------- rating

    /** Rule: điểm 1-5, mỗi user 1 rating/bài: gửi lại là cập nhật (upsert). */
    @Override
    @Transactional
    public RatingResponse rate(RatingRequest req) {
        Integer userId = SecurityUtils.requireUser().id();
        Rating rating = ratingRepository.findByUserIdAndTrackId(userId, req.trackId()).orElseGet(() -> {
            requireTrackExists(req.trackId());
            return Rating.builder().userId(userId).trackId(req.trackId()).build();
        });
        rating.setRatingValue(req.ratingValue());
        ratingRepository.save(rating);
        return new RatingResponse(req.trackId(), userId, req.ratingValue());
    }

    @Override
    @Transactional(readOnly = true)
    public RatingSummaryResponse ratingSummary(Integer trackId) {
        double avg = Math.round(ratingRepository.averageOf(trackId) * 10.0) / 10.0;
        return new RatingSummaryResponse(trackId, avg, ratingRepository.countByTrackId(trackId));
    }

    @Override
    @Transactional(readOnly = true)
    public RatingResponse myRating(Integer trackId) {
        Integer userId = SecurityUtils.requireUser().id();
        return ratingRepository.findByUserIdAndTrackId(userId, trackId)
                .map(r -> new RatingResponse(trackId, userId, r.getRatingValue()))
                .orElse(new RatingResponse(trackId, userId, null));
    }

    @Override
    @Transactional
    public void deleteMyRating(Integer trackId) {
        Rating rating = ratingRepository.findByUserIdAndTrackId(SecurityUtils.requireUser().id(), trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa đánh giá bài hát này"));
        ratingRepository.delete(rating);
    }

    private void requireTrackExists(Integer trackId) {
        if (contentClient.getTracks(List.of(trackId)).isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy bài hát id=" + trackId
                    + " (hoặc dịch vụ nội dung đang bận)");
        }
    }
}
