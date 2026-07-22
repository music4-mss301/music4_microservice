package vn.edu.fpt.sba.music4.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.sba.music4.dtos.request.SyncTimeRequest;
import vn.edu.fpt.sba.music4.dtos.request.TrackPlayRequest;
import vn.edu.fpt.sba.music4.dtos.response.TrackArtistInfo;
import vn.edu.fpt.sba.music4.dtos.response.TrackResponseDTO;
import vn.edu.fpt.sba.music4.entities.History;
import vn.edu.fpt.sba.music4.entities.Track;
import vn.edu.fpt.sba.music4.entities.TrackArtist;
import vn.edu.fpt.sba.music4.entities.User;
import vn.edu.fpt.sba.music4.exceptions.ResourceNotFoundException;
import vn.edu.fpt.sba.music4.repositories.HistoryRepository;
import vn.edu.fpt.sba.music4.repositories.TrackArtistRepository;
import vn.edu.fpt.sba.music4.repositories.TrackRepository;
import vn.edu.fpt.sba.music4.repositories.UserRepository;
import vn.edu.fpt.sba.music4.services.ITrackingService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingServiceImpl implements ITrackingService{

    private final TrackRepository trackRepository;
    private final HistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final TrackArtistRepository trackArtistRepository;

    // Cửa sổ chống trùng lượt nghe (giây), tính động theo thời lượng bài hát:
    // min(60s, duration * 0.7) —    bài ngắn thì cửa sổ ngắn tương ứng, tránh việc
    // 1 bài 20s vẫn bị "khóa" chống trùng tới 2 phút như cũ.
    private static final long MAX_DUPLICATE_VIEW_WINDOW_SECONDS = 60;
    private static final double DUPLICATE_VIEW_WINDOW_RATIO = 0.7;
    private static final long DEFAULT_DUPLICATE_VIEW_WINDOW_SECONDS = 30; // fallback khi duration null/invalid

    private long resolveDuplicateViewWindowSeconds(Track track) {
        Integer duration = track.getDuration();
        if (duration == null || duration <= 0) {
            return DEFAULT_DUPLICATE_VIEW_WINDOW_SECONDS;
        }
        double windowSeconds = duration * DUPLICATE_VIEW_WINDOW_RATIO;
        return Math.min(MAX_DUPLICATE_VIEW_WINDOW_SECONDS, (long) windowSeconds);
    }

    @Override
    @Async("trackingExecutor")
    @Transactional
    public void recordPlayHistory(Integer trackId, Integer userId, String currentUsername, String currentRole) {
        try {
            Track track = trackRepository.findById(trackId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bài hát"));

            boolean isArtistOrAdmin = currentRole != null
                    && (currentRole.toUpperCase().contains("ARTIST")
                    || currentRole.toUpperCase().contains("ADMIN"));
            String roleLog = currentRole != null ? currentRole : "N/A";

            if (!isArtistOrAdmin) {
                long windowSeconds = resolveDuplicateViewWindowSeconds(track);

                Optional<History> lastHistory = historyRepository
                        .findTopByUserIdAndTrackIdOrderByListenedAtDesc(userId, trackId);

                boolean countedRecently = lastHistory.isPresent()
                        && lastHistory.get().getListenedAt() != null
                        && lastHistory.get().getListenedAt()
                        .isAfter(LocalDateTime.now().minusSeconds(windowSeconds));

                if (countedRecently) {
                    log.info("[ANTI-DUP] Bỏ qua cộng view trùng lặp cho User {} - Track {} (đã ghi nhận cách đây < {}s, duration={}s)",
                            userId, trackId, windowSeconds, track.getDuration());
                } else {
                    trackRepository.incrementViewCount(trackId);
                    log.info("[SUCCESS] Đã cộng 1 view cho bài hát {}", trackId);

                    User user = userRepository.findById(userId).orElse(null);
                    if (user != null) {
                        History history = new History();
                        history.setUser(user);
                        history.setTrack(track);
                        history.setListenedAt(LocalDateTime.now());
                        historyRepository.save(history);
                    }
                }
            } else {
                log.info("[ANTI-CHEAT] BỎ QUA CỘNG VIEW & HISTORY: User {} (Role: {})",
                        currentUsername, roleLog);
            }

        } catch (Exception e) {
            log.error("Lỗi khi ghi lại lượt nghe cho bài hát {}: {}", trackId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void syncPlaybackPosition(SyncTimeRequest request) {
        try {
            Optional<History> latestHistory = historyRepository
                    .findTopByUserIdAndTrackIdOrderByListenedAtDesc(request.userId(), request.trackId());

            if (latestHistory.isPresent()) {
                // Đã có lịch sử -> Chỉ cần cập nhật thời gian
                History history = latestHistory.get();
                history.setPlaybackPosition(request.position());
                historyRepository.save(history);
            } else {
                log.warn("Bỏ qua Sync do chưa tìm thấy Session gốc (Luồng Async có thể đang xử lý). Track: {}", request.trackId());
            }
        } catch (Exception e) {
            log.error("Time synchronization error for Track {}: {}", request.trackId(), e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TrackResponseDTO> getListeningHistory(
            Integer userId,
            Pageable pageable) {

        Page<History> histories = historyRepository.findDistinctHistoryByUserId(userId, pageable);

        return histories.map(history -> {
            Track track = history.getTrack();
            List<TrackArtistInfo> artists = List.of();

            // Gọi Repo bằng ID của bài hát
            List<TrackArtist> dbTrackArtists = trackArtistRepository.findByTrackIdWithArtist(track.getId());

            if (dbTrackArtists != null && !dbTrackArtists.isEmpty()) {
                artists = dbTrackArtists.stream()
                        .map(ta -> new TrackArtistInfo(
                                ta.getArtist().getId(),
                                ta.getArtist().getName(),
                                ta.getRole()
                        )).toList();
            }
            return new TrackResponseDTO(
                    track.getId(),
                    track.getName(),
                    track.getAlbum() != null ? track.getAlbum().getName() : "Single",
                    track.getImg(),
                    track.getDuration(),
                    track.getFilePath(),
                    track.getPreviewPath(),
                    track.getViewCount(),
                    track.getUploadDate(),
                    history.getPlaybackPosition(),
                    artists
            );
        });
    }

    @Override
    @Transactional
    public void cleanupHistory() {
        try {
            LocalDateTime oneMonthsAgo = LocalDateTime.now().minusMonths(1);
            historyRepository.cleanupGarbageHistory(oneMonthsAgo);
            log.info("[CLEANUP] Đã dọn dẹp thành công các lịch sử rác (nghe < 10s hoặc cũ hơn 1 tháng).");
        } catch (Exception e) {
            log.error("[CLEANUP] Lỗi khi dọn dẹp lịch sử rác: {}", e.getMessage());
        }
    }
}
