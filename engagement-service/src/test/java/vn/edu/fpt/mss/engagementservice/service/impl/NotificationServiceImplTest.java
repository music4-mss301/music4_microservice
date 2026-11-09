package vn.edu.fpt.mss.engagementservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.fpt.mss.engagementservice.dto.event.TrackEventPayload;
import vn.edu.fpt.mss.engagementservice.dto.response.NotificationResponse;
import vn.edu.fpt.mss.engagementservice.entity.Notification;
import vn.edu.fpt.mss.engagementservice.exception.ForbiddenException;
import vn.edu.fpt.mss.engagementservice.repository.FollowRepository;
import vn.edu.fpt.mss.engagementservice.repository.NotificationRepository;
import vn.edu.fpt.mss.engagementservice.security.AuthUser;
import vn.edu.fpt.mss.engagementservice.security.SecurityUtils;
import vn.edu.fpt.mss.engagementservice.service.NotificationStreamService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock NotificationRepository notificationRepository;
    @Mock FollowRepository followRepository;
    @Mock NotificationStreamService stream;
    @InjectMocks NotificationServiceImpl notificationService;

    @Test
    void markRead_whenOwner_marksNotificationAsRead() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            AuthUser owner = new AuthUser(1, "user1", "USER");
            utilities.when(SecurityUtils::requireUser).thenReturn(owner);

            Notification notification = Notification.builder()
                    .id(10).userId(1).content("Bài hát mới").isRead(false).build();
            when(notificationRepository.findById(10)).thenReturn(Optional.of(notification));

            NotificationResponse res = notificationService.markRead(10);
            assertTrue(notification.isRead());
            assertTrue(res.isRead());
        }
    }

    @Test
    void markRead_whenNotOwner_throwsForbiddenException() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            AuthUser otherUser = new AuthUser(2, "user2", "USER");
            utilities.when(SecurityUtils::requireUser).thenReturn(otherUser);

            Notification notification = Notification.builder()
                    .id(10).userId(1).content("Bài hát mới").isRead(false).build();
            when(notificationRepository.findById(10)).thenReturn(Optional.of(notification));

            ForbiddenException ex = assertThrows(ForbiddenException.class, () -> notificationService.markRead(10));
            assertEquals("Đây không phải thông báo của bạn", ex.getMessage());
            assertFalse(notification.isRead());
        }
    }

    @Test
    void delete_whenNotOwner_throwsForbiddenException() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            AuthUser otherUser = new AuthUser(2, "user2", "USER");
            utilities.when(SecurityUtils::requireUser).thenReturn(otherUser);

            Notification notification = Notification.builder()
                    .id(10).userId(1).content("Bài hát mới").isRead(false).build();
            when(notificationRepository.findById(10)).thenReturn(Optional.of(notification));

            assertThrows(ForbiddenException.class, () -> notificationService.delete(10));
            verify(notificationRepository, never()).delete(any());
        }
    }

    @Test
    void listOf_whenOtherUserAndNotAdmin_throwsForbiddenException() {
        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class, CALLS_REAL_METHODS)) {
            AuthUser otherUser = new AuthUser(2, "user2", "USER");
            utilities.when(SecurityUtils::requireUser).thenReturn(otherUser);

            assertThrows(ForbiddenException.class, () -> notificationService.listOf(1));
            verify(notificationRepository, never()).findByUserIdOrderByCreatedAtDesc(any());
        }
    }

    @Test
    void notifyFollowersOfNewTrack_createsNotificationsAndPushesSse() {
        TrackEventPayload payload = new TrackEventPayload(
                100, "Chạy Ngay Đi", "images/cover.jpg",
                List.of(new TrackEventPayload.Ref(5, "Sơn Tùng M-TP"))
        );
        when(followRepository.findFollowerIds(List.of(5))).thenReturn(List.of(11, 12));
        when(notificationRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = notificationService.notifyFollowersOfNewTrack(payload);
        assertEquals(2, count);
        verify(stream).push(eq(11), eq("NEW_TRACK"), any(NotificationResponse.class));
        verify(stream).push(eq(12), eq("NEW_TRACK"), any(NotificationResponse.class));
    }
}
