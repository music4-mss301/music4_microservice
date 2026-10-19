package vn.edu.fpt.mss.engagementservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.mss.engagementservice.dto.event.TrackEventPayload;
import vn.edu.fpt.mss.engagementservice.dto.response.NotificationResponse;
import vn.edu.fpt.mss.engagementservice.dto.response.PageResponse;
import vn.edu.fpt.mss.engagementservice.entity.Notification;
import vn.edu.fpt.mss.engagementservice.exception.ForbiddenException;
import vn.edu.fpt.mss.engagementservice.exception.ResourceNotFoundException;
import vn.edu.fpt.mss.engagementservice.mapper.EngagementMapper;
import vn.edu.fpt.mss.engagementservice.repository.FollowRepository;
import vn.edu.fpt.mss.engagementservice.repository.NotificationRepository;
import vn.edu.fpt.mss.engagementservice.security.AuthUser;
import vn.edu.fpt.mss.engagementservice.security.SecurityUtils;
import vn.edu.fpt.mss.engagementservice.service.NotificationService;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final FollowRepository followRepository;

    /** Rule: chỉ chủ thông báo (hoặc ADMIN) xem được. */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getOf(Integer userId, int page, int size) {
        SecurityUtils.requireOwnerOrAdmin(userId);
        return PageResponse.of(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId,
                PageRequest.of(page, Math.min(size, 50))), EngagementMapper::toNotificationResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByUserIdAndIsReadFalse(SecurityUtils.requireUser().id());
    }

    @Override
    @Transactional
    public NotificationResponse markRead(Integer id) {
        Notification n = findOwned(id);
        n.setRead(true);
        return EngagementMapper.toNotificationResponse(n);
    }

    @Override
    @Transactional
    public int markAllRead() {
        return notificationRepository.markAllRead(SecurityUtils.requireUser().id());
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        notificationRepository.delete(findOwned(id));
    }

    /** Nhận event content.track.created: báo cho follower của mọi nghệ sĩ trong bài (mỗi người 1 thông báo). */
    @Override
    @Transactional
    public int notifyFollowersOfNewTrack(TrackEventPayload track) {
        if (track.artists() == null || track.artists().isEmpty()) {
            return 0;
        }
        List<Integer> artistIds = track.artists().stream().map(TrackEventPayload.Ref::id).toList();
        String artistNames = String.join(", ", track.artists().stream().map(TrackEventPayload.Ref::name).toList());
        Integer mainArtistId = artistIds.get(0);
        List<Notification> notifications = followRepository.findFollowerIds(artistIds).stream()
                .map(userId -> Notification.builder()
                        .userId(userId).artistId(mainArtistId).trackId(track.trackId())
                        .content(artistNames + " vừa phát hành bài hát mới: " + track.name())
                        .isRead(false).build())
                .toList();
        notificationRepository.saveAll(notifications);
        return notifications.size();
    }

    private Notification findOwned(Integer id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo id=" + id));
        AuthUser user = SecurityUtils.requireUser();
        if (!Objects.equals(n.getUserId(), user.id())) {
            throw new ForbiddenException("Đây không phải thông báo của bạn");
        }
        return n;
    }
}
