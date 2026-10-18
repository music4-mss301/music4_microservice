package vn.edu.fpt.mss.analyticsservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.mss.analyticsservice.client.ContentClient;
import vn.edu.fpt.mss.analyticsservice.client.EngagementClient;
import vn.edu.fpt.mss.analyticsservice.client.SearchClient;
import vn.edu.fpt.mss.analyticsservice.dto.client.Suggestion;
import vn.edu.fpt.mss.analyticsservice.dto.client.TrackSummary;
import vn.edu.fpt.mss.analyticsservice.dto.client.UserSignals;
import vn.edu.fpt.mss.analyticsservice.dto.request.RecommendQuery;
import vn.edu.fpt.mss.analyticsservice.dto.response.RecommendationResponse;
import vn.edu.fpt.mss.analyticsservice.dto.response.TrackPlayCount;
import vn.edu.fpt.mss.analyticsservice.mapper.AnalyticsMapper;
import vn.edu.fpt.mss.analyticsservice.repository.HistoryRepository;
import vn.edu.fpt.mss.analyticsservice.security.SecurityUtils;
import vn.edu.fpt.mss.analyticsservice.service.RecommendationService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Luồng gợi ý (xem Music4-refactor-events.md mục 6):
 * history (DB riêng) + signals (1 call engagement) -> kNN trên ES (1 call search-admin) -> fallback trending.
 * Không loop findAll() như monolith; mọi call ra ngoài đều có circuit breaker + fallback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final HistoryRepository historyRepository;
    private final EngagementClient engagementClient;
    private final SearchClient searchClient;
    private final ContentClient contentClient;

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationResponse> recommend(int size) {
        int limit = Math.min(Math.max(size, 1), 30);
        Integer userId = SecurityUtils.currentUserIdOrNull();
        if (userId == null) {
            return trending(limit, Set.of());
        }
        UserSignals signals = engagementClient.signals(userId);
        Set<Integer> liked = new LinkedHashSet<>(signals.favoriteTrackIds());
        liked.addAll(signals.likedTrackIds());
        liked.addAll(historyRepository.findRecentTrackIds(userId, PageRequest.of(0, 20)));
        Set<Integer> disliked = new LinkedHashSet<>(signals.dislikedTrackIds());
        liked.removeAll(disliked);

        if (!liked.isEmpty()) {
            Set<Integer> exclude = new LinkedHashSet<>(liked);
            exclude.addAll(disliked);
            List<Suggestion> suggestions = searchClient.recommend(new RecommendQuery(
                    new ArrayList<>(liked), new ArrayList<>(disliked), new ArrayList<>(exclude), limit));
            if (!suggestions.isEmpty()) {
                return suggestions.stream().map(AnalyticsMapper::fromSuggestion).toList();
            }
        }
        Set<Integer> exclude = new LinkedHashSet<>(disliked);
        return trending(limit, exclude);
    }

    /** Trending = nhiều lượt nghe nhất 7 ngày qua (DB riêng); chưa có dữ liệu thì lấy theo view_count của content. */
    private List<RecommendationResponse> trending(int limit, Set<Integer> exclude) {
        List<Integer> ids = historyRepository.topTracks(LocalDateTime.now().minusDays(7), PageRequest.of(0, limit * 2))
                .stream().map(TrackPlayCount::trackId).filter(id -> !exclude.contains(id)).limit(limit).toList();
        List<TrackSummary> tracks = ids.isEmpty() ? contentClient.trending(limit) : contentClient.getTracks(ids);
        return tracks.stream().filter(t -> !exclude.contains(t.id())).limit(limit)
                .map(AnalyticsMapper::fromTrending).toList();
    }
}
