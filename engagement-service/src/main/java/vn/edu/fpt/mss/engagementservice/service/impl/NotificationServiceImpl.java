package vn.edu.fpt.mss.engagementservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.mss.engagementservice.dto.event.AlbumEventPayload;
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
import vn.edu.fpt.mss.engagementservice.service.NotificationStreamService;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final FollowRepository followRepository;
    private final NotificationStreamService stream;

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
    public List<NotificationResponse> listOf(Integer userId) {
        SecurityUtils.requireOwnerOrAdmin(userId);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(EngagementMapper::toNotificationResponse).toList();
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

    @Override
    @Transactional
    public int clearAll() {
        return notificationRepository.deleteByUserId(SecurityUtils.requireUser().id());
    }

    /** content.track.created: mỗi follower của các nghệ sĩ trong bài nhận 1 thông báo và được đẩy realtime (NEW_TRACK). */
    @Override
    @Transactional
    public int notifyFollowersOfNewTrack(TrackEventPayload track) {
        if (track.artists() == null || track.artists().isEmpty()) {
            return 0;
        }
        List<Integer> artistIds = track.artists().stream().map(TrackEventPayload.Ref::id).toList();
        String artistNames = String.join(", ", track.artists().stream().map(TrackEventPayload.Ref::name).toList());
        List<Notification> saved = notificationRepository.saveAll(followRepository.findFollowerIds(artistIds).stream()
                .map(userId -> Notification.builder()
                        .userId(userId).artistId(artistIds.get(0)).artistName(artistNames)
                        .trackId(track.trackId()).trackName(track.name())
                        .content("Nghệ sĩ " + artistNames + " vừa phát hành bài hát mới: " + track.name())
                        .isRead(false).build())
                .toList());
        saved.forEach(n -> stream.push(n.getUserId(), "NEW_TRACK", EngagementMapper.toNotificationResponse(n)));
        return saved.size();
    }

    /** content.album.created: tương tự nhưng cho album (NEW_ALBUM). */
    @Override
    @Transactional
    public int notifyFollowersOfNewAlbum(AlbumEventPayload album) {
        if (album.artistId() == null) {
            return 0;
        }
        List<Notification> saved = notificationRepository.saveAll(followRepository.findFollowerIds(List.of(album.artistId()))
                .stream()
                .map(userId -> Notification.builder()
                        .userId(userId).artistId(album.artistId()).artistName(album.artistName())
                        .albumId(album.albumId()).albumName(album.name())
                        .content("Nghệ sĩ " + album.artistName() + " vừa phát hành album mới: " + album.name())
                        .isRead(false).build())
                .toList());
        saved.forEach(n -> stream.push(n.getUserId(), "NEW_ALBUM", EngagementMapper.toNotificationResponse(n)));
        return saved.size();
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
