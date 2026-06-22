package vn.edu.fpt.sba.music4.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.sba.music4.dtos.request.SyncTimeRequest;
import vn.edu.fpt.sba.music4.dtos.response.TrackResponseDTO;
import vn.edu.fpt.sba.music4.entities.History;
import vn.edu.fpt.sba.music4.entities.Track;
import vn.edu.fpt.sba.music4.entities.User;
import vn.edu.fpt.sba.music4.repositories.HistoryRepository;
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

    @Override
    @Async("trackingExecutor")
    @Transactional
    public void recordPlayHistory(Integer trackId, Integer userId) {
        try {
            trackRepository.incrementViewCount(trackId);

            History history = new History();

            Track trackRef = trackRepository.getReferenceById(trackId);
            history.setTrack(trackRef);

            if (userId != null) {
                User userRef = userRepository.getReferenceById(userId);
                history.setUser(userRef);
            }

            history.setListenedAt(LocalDateTime.now());
            historyRepository.save(history);

            log.info("Recorded history for Track {} by User {}", trackId, userId);
        } catch (Exception e) {
            log.error("Error recording play history for Track {}: {}", trackId, e.getMessage());
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
                // CHƯA CÓ LỊCH SỬ (Nghe lần đầu) -> TẠO MỚI
                log.info("Chưa có lịch sử, đang tạo mới cho User {} và Track {} tại giây {}",
                        request.userId(), request.trackId(), request.position());

                History newHistory = new History();
                Track trackRef = trackRepository.getReferenceById(request.trackId());
                User userRef = userRepository.getReferenceById(request.userId());

                newHistory.setTrack(trackRef);
                newHistory.setUser(userRef);
                newHistory.setListenedAt(LocalDateTime.now());
                newHistory.setPlaybackPosition(request.position());

                historyRepository.save(newHistory);
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

            return new TrackResponseDTO(
                    track.getId(),
                    track.getName(),
                    track.getImg(),
                    track.getDuration(),
                    track.getFilePath(),
                    track.getPreviewPath(),
                    track.getViewCount(),
                    track.getUploadDate(),
                    history.getPlaybackPosition()
            );
        });
    }

    @Override
    @Transactional
    public void cleanupHistory() {
        try {
            LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(1);
            historyRepository.cleanupGarbageHistory(sixMonthsAgo);
            log.info("[CLEANUP] Đã dọn dẹp thành công các lịch sử rác (nghe < 5s hoặc cũ hơn 1 tháng).");
        } catch (Exception e) {
            log.error("[CLEANUP] Lỗi khi dọn dẹp lịch sử rác: {}", e.getMessage());
        }
    }

}
