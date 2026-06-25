package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.fpt.sba.music4.dtos.response.NotificationResponseDTO;
import vn.edu.fpt.sba.music4.entities.Follow;
import vn.edu.fpt.sba.music4.entities.Notification;
import vn.edu.fpt.sba.music4.entities.Track;
import vn.edu.fpt.sba.music4.repositories.FollowRepository;
import vn.edu.fpt.sba.music4.repositories.NotificationRepository;
import vn.edu.fpt.sba.music4.services.INotificationService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final FollowRepository followRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getMyNotifications(Integer userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream().map(n -> new NotificationResponseDTO(
                n.getId(),
                n.getTrack() != null ? n.getTrack().getId() : null,
                n.getTrack() != null ? n.getTrack().getName() : null,
                n.getArtist() != null ? n.getArtist().getName() : null,
                n.getContent(),
                n.getIsRead(),
                n.getCreatedAt()
        )).toList();
    }

    @Override
    @Transactional
    public void markAsRead(Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo này."));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void sendNewTrackNotification(Track track) {
        // Kiểm tra nếu bài hát không có nghệ sĩ nào thì dừng lại
        if (track.getTrackArtists() == null || track.getTrackArtists().isEmpty()) {
            return;
        }

        for (vn.edu.fpt.sba.music4.entities.TrackArtist trackArtist : track.getTrackArtists()) {
            vn.edu.fpt.sba.music4.entities.Artist artist = trackArtist.getArtist();
            if (artist == null) continue;

            List<Follow> follows = followRepository.findByArtistId(artist.getId());

            for (Follow follow : follows) {
                Notification notification = new Notification();
                notification.setUser(follow.getUser());
                notification.setArtist(artist);
                notification.setTrack(track);
                notification.setContent("Nghệ sĩ " + artist.getName() + " vừa phát hành bài hát mới: " + track.getName());
                notification.setIsRead(false);
                notification.setCreatedAt(LocalDateTime.now());

                notificationRepository.save(notification);
            }
        }
    }
}