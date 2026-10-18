package vn.edu.fpt.mss.analyticsservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.mss.analyticsservice.client.ContentClient;
import vn.edu.fpt.mss.analyticsservice.client.EngagementClient;
import vn.edu.fpt.mss.analyticsservice.dto.client.ArtistEngagementStats;
import vn.edu.fpt.mss.analyticsservice.dto.client.ArtistStats;
import vn.edu.fpt.mss.analyticsservice.dto.client.TrackSummary;
import vn.edu.fpt.mss.analyticsservice.dto.response.*;
import vn.edu.fpt.mss.analyticsservice.exception.ForbiddenException;
import vn.edu.fpt.mss.analyticsservice.repository.HistoryRepository;
import vn.edu.fpt.mss.analyticsservice.security.AuthUser;
import vn.edu.fpt.mss.analyticsservice.security.SecurityUtils;
import vn.edu.fpt.mss.analyticsservice.service.StatisticsService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final HistoryRepository historyRepository;
    private final ContentClient contentClient;
    private final EngagementClient engagementClient;

    /** Tổng hợp từ 3 nguồn: content (view, danh sách bài), engagement (favorite, follower), analytics (lượt nghe). */
    @Override
    @Transactional(readOnly = true)
    public ArtistOverviewResponse artistOverview(Integer artistId) {
        ArtistStats content = requireArtistAccess(artistId);
        ArtistEngagementStats engagement = engagementClient.artistStats(artistId, content.trackIds());
        long plays30 = content.trackIds().isEmpty() ? 0
                : historyRepository.countPlaysOf(content.trackIds(), LocalDateTime.now().minusDays(30));
        return new ArtistOverviewResponse(artistId, content.totalViews(), engagement.totalFavorites(),
                engagement.totalFollowers(), content.trackIds().size(), plays30);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyCount> artistDailyViews(Integer artistId, int days) {
        ArtistStats content = requireArtistAccess(artistId);
        int range = Math.min(Math.max(days, 1), 365);
        if (content.trackIds().isEmpty()) {
            return fillDays(List.of(), range);
        }
        return fillDays(historyRepository.dailyPlaysOf(content.trackIds(),
                LocalDate.now().minusDays(range - 1L).atStartOfDay()), range);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOverviewResponse adminOverview(int days) {
        int range = Math.min(Math.max(days, 1), 365);
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        return new AdminOverviewResponse(historyRepository.count(),
                historyRepository.countByListenedAtAfter(LocalDate.now().atStartOfDay()),
                historyRepository.countByListenedAtAfter(weekAgo),
                historyRepository.countDistinctListeners(weekAgo),
                fillDays(historyRepository.dailyPlays(LocalDate.now().minusDays(range - 1L).atStartOfDay()), range));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopTrackResponse> topTracks(int days, int limit) {
        List<TrackPlayCount> top = historyRepository.topTracks(LocalDateTime.now().minusDays(Math.max(days, 1)),
                PageRequest.of(0, Math.min(Math.max(limit, 1), 50)));
        if (top.isEmpty()) {
            return List.of();
        }
        Map<Integer, TrackSummary> tracks = new HashMap<>();
        contentClient.getTracks(top.stream().map(TrackPlayCount::trackId).toList())
                .forEach(t -> tracks.put(t.id(), t));
        return top.stream().map(t -> new TopTrackResponse(t.trackId(), t.plays(), tracks.get(t.trackId()))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsStatsResponse stats() {
        return new AnalyticsStatsResponse(historyRepository.count(),
                historyRepository.countByListenedAtAfter(LocalDate.now().atStartOfDay()),
                historyRepository.countByListenedAtAfter(LocalDateTime.now().minusDays(7)));
    }

    /** Rule: chỉ chính nghệ sĩ đó hoặc ADMIN xem được số liệu của nghệ sĩ. */
    private ArtistStats requireArtistAccess(Integer artistId) {
        ArtistStats stats = contentClient.artistStats(artistId);
        AuthUser user = SecurityUtils.requireUser();
        if (!user.isAdmin() && !Objects.equals(user.id(), stats.userId())) {
            throw new ForbiddenException("Bạn chỉ xem được thống kê của chính mình");
        }
        return stats;
    }

    /** Điền 0 cho những ngày không có lượt nghe để biểu đồ không bị "nhảy cóc". */
    private List<DailyCount> fillDays(List<DailyCount> data, int days) {
        Map<LocalDate, Long> byDay = new HashMap<>();
        data.forEach(d -> byDay.put(d.day(), d.total()));
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        return start.datesUntil(LocalDate.now().plusDays(1))
                .map(d -> new DailyCount(d, byDay.getOrDefault(d, 0L))).toList();
    }
}
