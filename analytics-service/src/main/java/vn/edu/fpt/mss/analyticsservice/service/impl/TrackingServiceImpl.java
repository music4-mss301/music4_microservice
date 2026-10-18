package vn.edu.fpt.mss.analyticsservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.mss.analyticsservice.client.ContentClient;
import vn.edu.fpt.mss.analyticsservice.dto.client.TrackSummary;
import vn.edu.fpt.mss.analyticsservice.dto.request.SyncTimeRequest;
import vn.edu.fpt.mss.analyticsservice.dto.response.HistoryResponse;
import vn.edu.fpt.mss.analyticsservice.dto.response.PageResponse;
import vn.edu.fpt.mss.analyticsservice.dto.response.PlayResponse;
import vn.edu.fpt.mss.analyticsservice.entity.History;
import vn.edu.fpt.mss.analyticsservice.event.EventPublisher;
import vn.edu.fpt.mss.analyticsservice.event.EventTypes;
import vn.edu.fpt.mss.analyticsservice.exception.BadRequestException;
import vn.edu.fpt.mss.analyticsservice.exception.ResourceNotFoundException;
import vn.edu.fpt.mss.analyticsservice.mapper.AnalyticsMapper;
import vn.edu.fpt.mss.analyticsservice.repository.HistoryRepository;
import vn.edu.fpt.mss.analyticsservice.security.SecurityUtils;
import vn.edu.fpt.mss.analyticsservice.service.TrackingService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TrackingServiceImpl implements TrackingService {

    static final int MIN_RETENTION_DAYS = 30;

    private final HistoryRepository historyRepository;
    private final ContentClient contentClient;
    private final EventPublisher eventPublisher;

    /** Lấy từ Config Server (config-repo/analytics-service.yml) để demo đổi cấu hình tập trung. */
    @Value("${app.analytics.play-dedupe-seconds:30}")
    private int dedupeSeconds;

    /**
     * Rule: cùng user nghe lại cùng bài trong N giây (mặc định 30) thì không tính thêm lượt (chống spam view_count).
     * analytics KHÔNG ghi vào DB của content: chỉ bắn analytics.track.played, content tự cộng view.
     */
    @Override
    @Transactional
    public PlayResponse recordPlay(Integer trackId) {
        Integer userId = SecurityUtils.currentUserIdOrNull();
        if (userId != null && historyRepository.existsByUserIdAndTrackIdAndListenedAtAfter(
                userId, trackId, LocalDateTime.now().minusSeconds(dedupeSeconds))) {
            return new PlayResponse(trackId, false, "Lượt nghe trùng trong " + dedupeSeconds + " giây, bỏ qua");
        }
        historyRepository.save(History.builder().userId(userId).trackId(trackId).build());
        Map<String, Object> payload = new HashMap<>();
        payload.put("trackId", trackId);
        payload.put("userId", userId);
        eventPublisher.publish(EventTypes.TRACK_PLAYED, payload);
        return new PlayResponse(trackId, true, "Đã ghi nhận lượt nghe");
    }

    @Override
    @Transactional
    public void syncPosition(SyncTimeRequest req) {
        Integer userId = SecurityUtils.requireUser().id();
        History history = historyRepository.findTopByUserIdAndTrackIdOrderByListenedAtDesc(userId, req.trackId())
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có lượt nghe nào của bài này để đồng bộ"));
        history.setPlaybackPosition(req.position());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<HistoryResponse> myHistory(int page, int size) {
        Page<History> history = historyRepository.findByUserIdOrderByListenedAtDesc(SecurityUtils.requireUser().id(),
                PageRequest.of(page, Math.min(size, 50)));
        var ids = history.getContent().stream().map(History::getTrackId).distinct().toList();
        Map<Integer, TrackSummary> tracks = new HashMap<>();
        if (!ids.isEmpty()) {
            contentClient.getTracks(ids).forEach(t -> tracks.put(t.id(), t));
        }
        return PageResponse.of(history, h -> AnalyticsMapper.toHistoryResponse(h, tracks.get(h.getTrackId())));
    }

    /** Rule: chỉ chủ lịch sử hoặc ADMIN được xóa. */
    @Override
    @Transactional
    public void deleteEntry(Integer id) {
        History h = historyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch sử id=" + id));
        SecurityUtils.requireOwnerOrAdmin(h.getUserId());
        historyRepository.delete(h);
    }

    @Override
    @Transactional
    public int clearMine() {
        return historyRepository.deleteByUserId(SecurityUtils.requireUser().id());
    }

    /** Rule: không cho xóa dữ liệu mới hơn 30 ngày (để thống kê 30 ngày luôn đúng). */
    @Override
    @Transactional
    public int cleanupOlderThan(int days) {
        if (days < MIN_RETENTION_DAYS) {
            throw new BadRequestException("Chỉ được dọn lịch sử cũ hơn " + MIN_RETENTION_DAYS + " ngày");
        }
        return historyRepository.deleteOlderThan(LocalDateTime.now().minusDays(days));
    }

    @Override
    @Transactional
    public void anonymizeUser(Integer userId) {
        historyRepository.anonymize(userId);
    }
}
