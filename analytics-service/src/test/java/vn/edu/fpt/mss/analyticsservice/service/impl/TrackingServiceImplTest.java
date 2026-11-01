package vn.edu.fpt.mss.analyticsservice.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.fpt.mss.analyticsservice.client.ContentClient;
import vn.edu.fpt.mss.analyticsservice.dto.request.SyncTimeRequest;
import vn.edu.fpt.mss.analyticsservice.dto.response.PlayResponse;
import vn.edu.fpt.mss.analyticsservice.entity.History;
import vn.edu.fpt.mss.analyticsservice.event.EventPublisher;
import vn.edu.fpt.mss.analyticsservice.event.EventTypes;
import vn.edu.fpt.mss.analyticsservice.exception.BadRequestException;
import vn.edu.fpt.mss.analyticsservice.exception.ForbiddenException;
import vn.edu.fpt.mss.analyticsservice.exception.ResourceNotFoundException;
import vn.edu.fpt.mss.analyticsservice.repository.HistoryRepository;
import vn.edu.fpt.mss.analyticsservice.security.AuthUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Business rule của luồng ghi lượt nghe (không cần DB, RabbitMQ hay service khác: tất cả được giả lập bằng Mockito). */
@ExtendWith(MockitoExtension.class)
class TrackingServiceImplTest {

    @Mock
    HistoryRepository historyRepository;
    @Mock
    ContentClient contentClient;
    @Mock
    EventPublisher eventPublisher;
    @InjectMocks
    TrackingServiceImpl service;

    @BeforeEach
    void setUp() {
        // @Value không được nạp khi chạy test thuần, đặt giá trị như config-repo (30 giây)
        ReflectionTestUtils.setField(service, "dedupeSeconds", 30);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void login(Integer id, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AuthUser(id, "user" + id, role), null, List.of()));
    }

    // ---------------------------------------------------------------- Rule: chống spam lượt nghe

    @Test
    void recordPlay_sameUserSameTrackWithinWindow_isIgnored() {
        login(2, "LISTENER");
        when(historyRepository.existsByUserIdAndTrackIdAndListenedAtAfter(eq(2), eq(10), any(LocalDateTime.class)))
                .thenReturn(true);

        PlayResponse res = service.recordPlay(10);

        assertFalse(res.counted());
        verify(historyRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any(), any());
    }

    @Test
    void recordPlay_firstListen_savesHistoryAndPublishesPlayedEvent() {
        login(2, "LISTENER");
        when(historyRepository.existsByUserIdAndTrackIdAndListenedAtAfter(eq(2), eq(10), any(LocalDateTime.class)))
                .thenReturn(false);

        PlayResponse res = service.recordPlay(10);

        assertTrue(res.counted());
        ArgumentCaptor<History> saved = ArgumentCaptor.forClass(History.class);
        verify(historyRepository).save(saved.capture());
        assertEquals(2, saved.getValue().getUserId());
        assertEquals(10, saved.getValue().getTrackId());

        // analytics KHÔNG tự cộng view_count (nằm ở DB của content), chỉ bắn event để content tự cộng
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(eq(EventTypes.TRACK_PLAYED), payload.capture());
        assertEquals(10, ((Map<?, ?>) payload.getValue()).get("trackId"));
    }

    @Test
    void recordPlay_guest_skipsDuplicateCheckAndStoresNullUser() {
        PlayResponse res = service.recordPlay(10);

        assertTrue(res.counted());
        verify(historyRepository, never()).existsByUserIdAndTrackIdAndListenedAtAfter(any(), any(), any());
        ArgumentCaptor<History> saved = ArgumentCaptor.forClass(History.class);
        verify(historyRepository).save(saved.capture());
        assertNull(saved.getValue().getUserId());
    }

    // ---------------------------------------------------------------- Rule: dọn lịch sử

    @Test
    void cleanupOlderThan_lessThan30Days_isRejected() {
        assertThrows(BadRequestException.class, () -> service.cleanupOlderThan(7));
        verifyNoInteractions(historyRepository);
    }

    @Test
    void cleanupOlderThan_30Days_deletesAndReturnsCount() {
        when(historyRepository.deleteOlderThan(any(LocalDateTime.class))).thenReturn(5);

        assertEquals(5, service.cleanupOlderThan(30));
    }

    // ---------------------------------------------------------------- Rule: chỉ chủ lịch sử hoặc ADMIN được xóa

    @Test
    void deleteEntry_byAnotherUser_isForbidden() {
        login(3, "LISTENER");
        History h = History.builder().id(1).userId(2).trackId(10).build();
        when(historyRepository.findById(1)).thenReturn(Optional.of(h));

        assertThrows(ForbiddenException.class, () -> service.deleteEntry(1));
        verify(historyRepository, never()).delete(any());
    }

    @Test
    void deleteEntry_byOwner_deletes() {
        login(2, "LISTENER");
        History h = History.builder().id(1).userId(2).trackId(10).build();
        when(historyRepository.findById(1)).thenReturn(Optional.of(h));

        service.deleteEntry(1);

        verify(historyRepository).delete(h);
    }

    @Test
    void deleteEntry_byAdmin_deletes() {
        login(1, "ADMIN");
        History h = History.builder().id(1).userId(2).trackId(10).build();
        when(historyRepository.findById(1)).thenReturn(Optional.of(h));

        service.deleteEntry(1);

        verify(historyRepository).delete(h);
    }

    @Test
    void deleteEntry_notFound_throwsResourceNotFound() {
        login(2, "LISTENER");
        when(historyRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteEntry(99));
    }

    // ---------------------------------------------------------------- Rule: đồng bộ vị trí nghe dở

    @Test
    void syncPosition_withoutAnyListen_isNotFound() {
        login(2, "LISTENER");
        when(historyRepository.findTopByUserIdAndTrackIdOrderByListenedAtDesc(2, 10)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.syncPosition(new SyncTimeRequest(10, 30)));
    }

    @Test
    void syncPosition_updatesLatestHistory() {
        login(2, "LISTENER");
        History h = History.builder().id(1).userId(2).trackId(10).build();
        when(historyRepository.findTopByUserIdAndTrackIdOrderByListenedAtDesc(2, 10)).thenReturn(Optional.of(h));

        service.syncPosition(new SyncTimeRequest(10, 95));

        assertEquals(95, (int) h.getPlaybackPosition());
    }
}
