package vn.edu.fpt.sba.music4.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.edu.fpt.sba.music4.dtos.response.TrackSuggestResponse;
import vn.edu.fpt.sba.music4.entities.Rating;
import vn.edu.fpt.sba.music4.entities.Track;
import vn.edu.fpt.sba.music4.repositories.FavoriteRepository;
import vn.edu.fpt.sba.music4.repositories.HistoryRepository;
import vn.edu.fpt.sba.music4.repositories.RatingRepository;
import vn.edu.fpt.sba.music4.repositories.TrackRepository;
import vn.edu.fpt.sba.music4.services.IRecommendationService;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements IRecommendationService {

    private final TrackRepository trackRepository;
    private final RatingRepository ratingRepository;
    private final FavoriteRepository favoriteRepository;
    private final HistoryRepository historyRepository;

    @Override
    public List<TrackSuggestResponse> getRecommendations(Integer userId) {
        // 1. Dành cho Guest (Chưa đăng nhập): Chỉ trả về Trending Tracks
        if (userId == null) {
            return getTrendingFallback();
        }

        // ==========================================
        // PHASE 1: TẠO HỒ SƠ GU ÂM NHẠC (TASTE PROFILE)
        // ==========================================
        Set<Integer> likedTrackIds = new HashSet<>();

        // A. Lấy bài hát đã đánh giá >= 4 sao
        ratingRepository.findByUserId(userId).stream()
                .filter(r -> r.getRatingValue() >= 4)
                .forEach(r -> likedTrackIds.add(r.getTrack().getId()));

        // B. Lấy bài hát đã thả tim (Favorites)
        favoriteRepository.findByUserId(userId)
                .forEach(f -> likedTrackIds.add(f.getTrack().getId()));

        // C. Lấy bài hát trong Lịch sử nghe (History)
        historyRepository.findByUserId(userId)
                .forEach(h -> likedTrackIds.add(h.getTrack().getId()));

        // Nếu User hoàn toàn mới (chưa có data), trả về Trending
        if (likedTrackIds.isEmpty()) {
            return getTrendingFallback();
        }

        // Từ danh sách bài hát yêu thích, rút trích ra ID Thể loại và ID Ca sĩ họ hay nghe
        Set<Integer> favoriteCategoryIds = new HashSet<>();
        Set<Integer> favoriteArtistIds = new HashSet<>();
        List<Track> likedTracks = trackRepository.findAllById(likedTrackIds);

        for (Track t : likedTracks) {
            t.getTrackCategories().forEach(tc -> favoriteCategoryIds.add(tc.getCategory().getId()));
            t.getTrackArtists().forEach(ta -> favoriteArtistIds.add(ta.getArtist().getId()));
        }

        // ==========================================
        // PHASE 2: CHẤM ĐIỂM AI (SCORING)
        // ==========================================
        List<Track> allTracks = trackRepository.findAll();
        List<TrackSuggestResponse> recommendations = new ArrayList<>();

        // Tìm số lượt view cao nhất để chuẩn hóa (Normalize) điểm Trending
        int maxViews = allTracks.stream().mapToInt(Track::getViewCount).max().orElse(1);

        for (Track track : allTracks) {
            double aiScore = 0.0;

            // Tiêu chí 1: Trùng Thể loại (Gu)
            boolean matchCategory = track.getTrackCategories().stream()
                    .anyMatch(tc -> favoriteCategoryIds.contains(tc.getCategory().getId()));
            if (matchCategory) aiScore += 3.0;

            // Tiêu chí 2: Trùng Ca sĩ (Gu)
            boolean matchArtist = track.getTrackArtists().stream()
                    .anyMatch(ta -> favoriteArtistIds.contains(ta.getArtist().getId()));
            if (matchArtist) aiScore += 4.0;

            // Tiêu chí 3: Trọng số phổ biến (Bài càng hot càng dễ được gợi ý)
            double viewBonus = ((double) track.getViewCount() / maxViews) * 2.0;
            aiScore += viewBonus;

            // Tiêu chí 4: Hệ thống Phạt (Penalty)
            // Nếu User từng ghét bài này (Rate 1-2 sao), trừ điểm cực mạnh
            Optional<Rating> userRating = ratingRepository.findByUserIdAndTrackId(userId, track.getId());
            if (userRating.isPresent() && userRating.get().getRatingValue() <= 2) {
                aiScore -= 10.0;
            }

            // Tiêu chí 5: Tránh gợi ý lại bài vừa nghe quá nhiều (Tùy chọn)
            // Nếu bài này đã nằm trong History/Favorites thì điểm giảm đi 1 chút để nhường chỗ cho bài mới
            if (likedTrackIds.contains(track.getId())) {
                aiScore -= 1.0;
            }

            // ==========================================
            // PHASE 3: ĐÓNG GÓI KẾT QUẢ
            // ==========================================
            // Lấy những bài có điểm AI > 1.0 (Nghĩa là có liên quan đến Gu)
            if (aiScore > 1.0) {
                recommendations.add(new TrackSuggestResponse(
                        track.getId(),
                        track.getName(),
                        track.getImg(),
                        track.getDuration(),
                        track.getPreviewPath(),
                        track.getViewCount(),
                        Math.round(aiScore * 10.0) / 10.0 // Làm tròn 1 chữ số thập phân
                ));
            }
        }

        // Xếp hạng: Sắp xếp giảm dần theo điểm AI (Thằng nào điểm cao nhất nổi lên đầu)
        recommendations.sort((t1, t2) -> Double.compare(t2.matchScore(), t1.matchScore()));

        // Trả về Top 10 bài xuất sắc nhất
        return recommendations.stream().limit(10).collect(Collectors.toList());
    }

    // Hàm dự phòng (Fallback) khi User không có data
    private List<TrackSuggestResponse> getTrendingFallback() {
        return trackRepository.findAll().stream()
                .sorted((t1, t2) -> Integer.compare(t2.getViewCount(), t1.getViewCount()))
                .limit(10)
                .map(track -> new TrackSuggestResponse(
                        track.getId(),
                        track.getName(),
                        track.getImg(),
                        track.getDuration(),
                        track.getPreviewPath(),
                        track.getViewCount(),
                        99.9 // Điểm ảo định danh đây là nhạc Trending
                )).collect(Collectors.toList());
    }
}