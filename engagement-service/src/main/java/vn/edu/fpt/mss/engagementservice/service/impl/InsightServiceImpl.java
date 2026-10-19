package vn.edu.fpt.mss.engagementservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.mss.engagementservice.dto.response.ArtistEngagementStats;
import vn.edu.fpt.mss.engagementservice.dto.response.EngagementStatsResponse;
import vn.edu.fpt.mss.engagementservice.dto.response.UserSignalsResponse;
import vn.edu.fpt.mss.engagementservice.entity.Favorite;
import vn.edu.fpt.mss.engagementservice.entity.Rating;
import vn.edu.fpt.mss.engagementservice.repository.*;
import vn.edu.fpt.mss.engagementservice.service.InsightService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsightServiceImpl implements InsightService {

    private final FavoriteRepository favoriteRepository;
    private final RatingRepository ratingRepository;
    private final FollowRepository followRepository;
    private final CommentRepository commentRepository;
    private final PlaylistRepository playlistRepository;
    private final PlaylistTrackRepository playlistTrackRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public UserSignalsResponse signals(Integer userId) {
        List<Integer> favorites = favoriteRepository.findByUserIdOrderByLikedAtDesc(userId).stream()
                .map(Favorite::getTrackId).toList();
        List<Rating> ratings = ratingRepository.findByUserId(userId);
        List<Integer> liked = ratings.stream().filter(r -> r.getRatingValue() >= 4).map(Rating::getTrackId).toList();
        List<Integer> disliked = ratings.stream().filter(r -> r.getRatingValue() <= 2).map(Rating::getTrackId).toList();
        return new UserSignalsResponse(userId, favorites, liked, disliked);
    }

    @Override
    @Transactional(readOnly = true)
    public ArtistEngagementStats artistStats(Integer artistId, List<Integer> trackIds) {
        long favorites = trackIds == null || trackIds.isEmpty() ? 0 : favoriteRepository.countByTrackIdIn(trackIds);
        return new ArtistEngagementStats(artistId, favorites, followRepository.countByArtistId(artistId));
    }

    @Override
    @Transactional(readOnly = true)
    public EngagementStatsResponse stats() {
        return new EngagementStatsResponse(playlistRepository.count(), favoriteRepository.count(),
                followRepository.count(), ratingRepository.count(), commentRepository.count());
    }

    @Override
    @Transactional
    public void cleanupTrack(Integer trackId) {
        int n = playlistTrackRepository.deleteByTrackId(trackId) + favoriteRepository.deleteByTrackId(trackId)
                + ratingRepository.deleteByTrackId(trackId) + commentRepository.deleteByTrackId(trackId);
        log.info("Dọn {} bản ghi engagement của track {}", n, trackId);
    }

    @Override
    @Transactional
    public void cleanupArtist(Integer artistId) {
        log.info("Xóa {} follow của artist {}", followRepository.deleteByArtistId(artistId), artistId);
    }

    @Override
    @Transactional
    public void cleanupUser(Integer userId) {
        favoriteRepository.deleteByUserId(userId);
        ratingRepository.deleteByUserId(userId);
        followRepository.deleteByUserId(userId);
        commentRepository.deleteByUserId(userId);
        notificationRepository.deleteByUserId(userId);
        playlistRepository.deleteByUserId(userId);
        log.info("Đã dọn dữ liệu engagement của user {}", userId);
    }
}
