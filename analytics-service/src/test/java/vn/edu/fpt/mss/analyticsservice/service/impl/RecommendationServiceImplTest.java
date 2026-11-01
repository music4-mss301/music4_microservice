package vn.edu.fpt.mss.analyticsservice.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.fpt.mss.analyticsservice.client.ContentClient;
import vn.edu.fpt.mss.analyticsservice.client.EngagementClient;
import vn.edu.fpt.mss.analyticsservice.client.SearchClient;
import vn.edu.fpt.mss.analyticsservice.dto.client.Suggestion;
import vn.edu.fpt.mss.analyticsservice.dto.client.TrackSummary;
import vn.edu.fpt.mss.analyticsservice.dto.client.UserSignals;
import vn.edu.fpt.mss.analyticsservice.dto.request.RecommendQuery;
import vn.edu.fpt.mss.analyticsservice.dto.response.RecommendationResponse;
import vn.edu.fpt.mss.analyticsservice.dto.response.TrackPlayCount;
import vn.edu.fpt.mss.analyticsservice.repository.HistoryRepository;
import vn.edu.fpt.mss.analyticsservice.security.AuthUser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Luồng gợi ý: lịch sử + tín hiệu từ engagement, kNN ở search-admin, rơi về trending khi thiếu dữ liệu. */
@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @Mock
    HistoryRepository historyRepository;
    @Mock
    EngagementClient engagementClient;
    @Mock
    SearchClient searchClient;
    @Mock
    ContentClient contentClient;
    @InjectMocks
    RecommendationServiceImpl service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void login(Integer id) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AuthUser(id, "user" + id, "LISTENER"), null, List.of()));
    }

    private static TrackSummary track(int id) {
        return new TrackSummary(id, "Bài " + id, null, 180, null, null, 100L, LocalDate.now(), null, null, List.of());
    }

    @Test
    void guest_getsTrendingOnly_withoutCallingEngagementOrSearch() {
        when(historyRepository.topTracks(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(new TrackPlayCount(1, 10L), new TrackPlayCount(2, 5L)));
        when(contentClient.getTracks(List.of(1, 2))).thenReturn(List.of(track(1), track(2)));

        List<RecommendationResponse> result = service.recommend(5);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> "TRENDING".equals(r.source())));
        verifyNoInteractions(engagementClient, searchClient);
    }

    @Test
    void userWithoutHistoryOrSignals_fallsBackToContentTrending() {
        login(2);
        when(engagementClient.signals(2)).thenReturn(UserSignals.empty(2));
        when(historyRepository.findRecentTrackIds(eq(2), any(Pageable.class))).thenReturn(List.of());
        when(historyRepository.topTracks(any(LocalDateTime.class), any(Pageable.class))).thenReturn(List.of());
        when(contentClient.trending(5)).thenReturn(List.of(track(7), track(8)));

        List<RecommendationResponse> result = service.recommend(5);

        assertEquals(List.of(7, 8), result.stream().map(RecommendationResponse::trackId).toList());
        assertTrue(result.stream().allMatch(r -> "TRENDING".equals(r.source())));
        verifyNoInteractions(searchClient);
    }

    @Test
    void personalized_usesLikedAndRecentTracks_excludesDisliked_andMarksPersonal() {
        login(2);
        // favorites [1] + liked [2] + nghe gần đây [4], bỏ bài đã dislike [3]
        when(engagementClient.signals(2)).thenReturn(new UserSignals(2, List.of(1), List.of(2), List.of(3)));
        when(historyRepository.findRecentTrackIds(eq(2), any(Pageable.class))).thenReturn(List.of(4));
        when(searchClient.recommend(any(RecommendQuery.class)))
                .thenReturn(List.of(new Suggestion(9, "Bài 9", null, 200, null, 50L, 0.91)));

        List<RecommendationResponse> result = service.recommend(10);

        assertEquals(1, result.size());
        assertEquals("PERSONAL", result.get(0).source());
        assertEquals(0.91, result.get(0).matchScore());

        ArgumentCaptor<RecommendQuery> query = ArgumentCaptor.forClass(RecommendQuery.class);
        verify(searchClient).recommend(query.capture());
        assertEquals(List.of(1, 2, 4), query.getValue().likedTrackIds());
        assertEquals(List.of(3), query.getValue().dislikedTrackIds());
        assertTrue(query.getValue().excludeTrackIds().containsAll(List.of(1, 2, 3, 4)),
                "không được gợi ý lại bài đã thích, đã nghe gần đây hoặc đã dislike");
    }

    @Test
    void searchReturnsNothing_fallsBackToTrendingWithoutDislikedTracks() {
        login(2);
        when(engagementClient.signals(2)).thenReturn(new UserSignals(2, List.of(1), List.of(), List.of(3)));
        when(historyRepository.findRecentTrackIds(eq(2), any(Pageable.class))).thenReturn(List.of());
        when(searchClient.recommend(any(RecommendQuery.class))).thenReturn(List.of());
        when(historyRepository.topTracks(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(new TrackPlayCount(3, 9L), new TrackPlayCount(5, 8L)));
        when(contentClient.getTracks(List.of(5))).thenReturn(List.of(track(5)));

        List<RecommendationResponse> result = service.recommend(10);

        assertEquals(List.of(5), result.stream().map(RecommendationResponse::trackId).toList());
        assertEquals("TRENDING", result.get(0).source());
    }

    @Test
    void size_isClampedBetween1And30() {
        when(historyRepository.topTracks(any(LocalDateTime.class), any(Pageable.class))).thenReturn(List.of());
        when(contentClient.trending(30)).thenReturn(List.of());
        when(contentClient.trending(1)).thenReturn(List.of());

        service.recommend(100);
        service.recommend(0);

        verify(contentClient).trending(30);
        verify(contentClient).trending(1);
    }
}
