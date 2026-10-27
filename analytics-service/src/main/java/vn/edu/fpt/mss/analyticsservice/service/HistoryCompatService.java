package vn.edu.fpt.mss.analyticsservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.mss.analyticsservice.client.ContentClient;
import vn.edu.fpt.mss.analyticsservice.dto.client.TrackSummary;
import vn.edu.fpt.mss.analyticsservice.dto.response.PageResponse;
import vn.edu.fpt.mss.analyticsservice.entity.History;
import vn.edu.fpt.mss.analyticsservice.repository.HistoryRepository;
import vn.edu.fpt.mss.analyticsservice.security.SecurityUtils;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Trang "Lịch sử nghe" của FE đọc danh sách BÀI HÁT (không phải bản ghi lịch sử): content, page.totalPages. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistoryCompatService {

    private final HistoryRepository historyRepository;
    private final ContentClient contentClient;

    /** Rule: chỉ chủ lịch sử hoặc ADMIN. Một bài nghe nhiều lần chỉ hiện một lần trong trang. */
    public PageResponse<TrackSummary> historyTracks(Integer userId, int page, int size) {
        SecurityUtils.requireOwnerOrAdmin(userId);
        int s = Math.min(Math.max(size, 1), 50);
        int p = Math.max(page, 0);
        Page<History> history = historyRepository.findByUserIdOrderByListenedAtDesc(userId, PageRequest.of(p, s));
        List<Integer> ids = history.getContent().stream().map(History::getTrackId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)).stream().toList();
        Map<Integer, TrackSummary> byId = new HashMap<>();
        if (!ids.isEmpty()) {
            contentClient.getTracks(ids).forEach(t -> byId.put(t.id(), t));
        }
        List<TrackSummary> tracks = ids.stream().map(byId::get).filter(Objects::nonNull).toList();
        return PageResponse.of(tracks, p, s, history.getTotalElements());
    }
}
