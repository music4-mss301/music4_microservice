package vn.edu.fpt.sba.music4.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import vn.edu.fpt.sba.music4.dtos.response.TrackArtistInfo;
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
                History history = latestHistory.get();
                history.setPlaybackPosition(request.position());
            } else {
                log.warn("No history found to synchronize time for User {} and Track {}", request.userId(), request.trackId());
            }
        } catch (Exception e) {
            log.error("Time synchronization error for Track {}: {}", request.trackId(), e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackResponseDTO> getListeningHistory(Integer userId) {
        // Lấy lịch sử từ DB, có thể thêm limit() để lấy top 20 bài gần nhất
        List<History> histories = historyRepository.findByUserId(userId);

        return histories.stream()
                .sorted((h1, h2) -> h2.getListenedAt().compareTo(h1.getListenedAt())) // Sắp xếp mới nhất lên đầu
                .map(history -> {
                    Track track = history.getTrack();

                    // 🟢 BƯỚC MỚI: Bóc tách danh sách nghệ sĩ từ bảng trung gian
                    List<TrackArtistInfo> artistList = track.getTrackArtists() != null ?
                            track.getTrackArtists().stream()
                                    .map(ta -> new TrackArtistInfo(
                                            ta.getArtist().getId(),
                                            ta.getArtist().getName(),
                                            ta.getRole()
                                    )).toList() : List.of();

                    return new TrackResponseDTO(
                            track.getId(),
                            track.getName(),
                            track.getImg(),
                            track.getDuration(),
                            track.getFilePath(),
                            track.getPreviewPath(),
                            track.getViewCount(),
                            track.getUploadDate(),
                            artistList // 🟢 Truyền danh sách nghệ sĩ vào tham số thứ 9
                    );
                })
                .toList();
    }

}
