package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.edu.fpt.sba.music4.dtos.response.NotificationResponseDTO;
import vn.edu.fpt.sba.music4.entities.Follow;
import vn.edu.fpt.sba.music4.entities.Notification;
import vn.edu.fpt.sba.music4.entities.Track;
import vn.edu.fpt.sba.music4.repositories.FollowRepository;
import vn.edu.fpt.sba.music4.repositories.NotificationRepository;
import vn.edu.fpt.sba.music4.repositories.TrackArtistRepository;
import vn.edu.fpt.sba.music4.services.INotificationService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final FollowRepository followRepository;
    private final TrackArtistRepository trackArtistRepository;

    private static final Map<Integer, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Integer userId) {
        SseEmitter emitter = new SseEmitter(24 * 60 * 60 * 1000L);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError((e) -> emitters.remove(userId));

        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected successfully!"));
        } catch (IOException e) {
            emitters.remove(userId);
        }

        return emitter;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getMyNotifications(Integer userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return notifications.stream().map(n -> {
            Integer trackId = null;
            String trackName = null;
            Integer albumId = null;
            String albumName = null;
            String artistName = null;

            try {
                if (n.getTrack() != null) {
                    trackId = n.getTrack().getId();
                    trackName = n.getTrack().getName();
                }
            } catch (Exception e) {
                trackId = null;
                trackName = "Bài hát không còn tồn tại";
            }

            try {
                if (n.getAlbum() != null) {
                    albumId = n.getAlbum().getId();
                    albumName = n.getAlbum().getName();
                }
            } catch (Exception e) {
                albumId = null;
                albumName = "Album không còn tồn tại";
            }

            try {
                if (n.getArtist() != null) {
                    artistName = n.getArtist().getName();
                }
            } catch (Exception e) {
                artistName = "Nghệ sĩ";
            }

            return new NotificationResponseDTO(
                    n.getId(),
                    trackId,
                    trackName,
                    albumId,
                    albumName,
                    artistName,
                    n.getContent(),
                    n.getIsRead(),
                    n.getCreatedAt()
            );
        }).toList();
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
    public void saveAndSend(Notification notification) {
        Notification savedNoti = notificationRepository.save(notification);
        Integer targetUserId = null;
        if (savedNoti.getUser() != null) {
            targetUserId = savedNoti.getUser().getId();
        } else if (savedNoti.getArtist() != null) {
            targetUserId = savedNoti.getArtist().getId();
        }

        if (targetUserId != null && emitters.containsKey(targetUserId)) {
            try {
                NotificationResponseDTO responseDTO = new NotificationResponseDTO(
                        savedNoti.getId(),
                        savedNoti.getTrack() != null ? savedNoti.getTrack().getId() : null,
                        savedNoti.getTrack() != null ? savedNoti.getTrack().getName() : null,
                        savedNoti.getAlbum() != null ? savedNoti.getAlbum().getId() : null,
                        savedNoti.getAlbum() != null ? savedNoti.getAlbum().getName() : null,
                        savedNoti.getArtist() != null ? savedNoti.getArtist().getName() : null,
                        savedNoti.getContent(),
                        savedNoti.getIsRead(),
                        savedNoti.getCreatedAt()
                );

                emitters.get(targetUserId).send(
                        SseEmitter.event()
                                .name("NOTIFICATION")
                                .data(responseDTO)
                );
            } catch (IOException e) {
                emitters.remove(targetUserId);
            }
        }
    }

    @Override
    @Transactional
    public void sendNewTrackNotification(Track track) {
        if (track == null) return;

        List<vn.edu.fpt.sba.music4.entities.TrackArtist> trackArtists = trackArtistRepository.findByTrackId(track.getId());
        if (trackArtists == null || trackArtists.isEmpty()) return;

        for (vn.edu.fpt.sba.music4.entities.TrackArtist trackArtist : trackArtists) {
            vn.edu.fpt.sba.music4.entities.Artist artist = trackArtist.getArtist();
            if (artist == null) continue;

            List<Follow> follows = followRepository.findByArtistId(artist.getId());
            if (follows == null || follows.isEmpty()) continue;

            for (Follow follow : follows) {
                Notification notification = new Notification();
                notification.setUser(follow.getUser());
                notification.setArtist(artist);
                notification.setTrack(track);
                notification.setContent("Nghệ sĩ " + artist.getName() + " vừa phát hành bài hát mới: " + track.getName());
                notification.setIsRead(false);
                notification.setCreatedAt(LocalDateTime.now());

                Notification savedNoti = notificationRepository.save(notification);

                Integer targetUserId = follow.getUser().getId();
                if (emitters.containsKey(targetUserId)) {
                    try {
                        NotificationResponseDTO responseDTO = new NotificationResponseDTO(
                                savedNoti.getId(),
                                track.getId(),
                                track.getName(),
                                null, // albumId = null vì đây là track mới
                                null, // albumName = null
                                artist.getName(),
                                savedNoti.getContent(),
                                savedNoti.getIsRead(),
                                savedNoti.getCreatedAt()
                        );

                        emitters.get(targetUserId).send(
                                SseEmitter.event()
                                        .name("NEW_TRACK")
                                        .data(responseDTO)
                        );
                    } catch (IOException e) {
                        emitters.remove(targetUserId);
                    }
                }
            }
        }
    }

    @Override
    public void createNewTrackNotification(Integer artistId, String artistName, Integer trackId, String trackName) {
        // Bỏ trống
    }

    @Override
    @Transactional
    public void deleteNotification(Integer notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new RuntimeException("Không tìm thấy thông báo để xóa.");
        }
        notificationRepository.deleteById(notificationId);
    }

    @Override
    @Transactional
    public void deleteAllMyNotifications(Integer userId) {
        notificationRepository.deleteByUserId(userId);
    }
}