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
import vn.edu.fpt.sba.music4.repositories.TrackArtistRepository; // 🟢 Nhớ import cái này nha bồ
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
    // 🟢 THÊM "final" Ở ĐÂY để @RequiredArgsConstructor tự động Inject vào constructor:
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
        System.out.println("=== [TEST NOTI] 1. Hàm sendNewTrackNotification đã chạy ===");
        if (track == null) {
            System.out.println("=== [TEST NOTI] LỖI: Đối tượng Track truyền vào bị NULL! ===");
            return;
        }
        System.out.println("=== [TEST NOTI] Tên bài hát: " + track.getName() + " | ID: " + track.getId());

        // 🟢 Đã hết lỗi đỏ vì đã có Repo bốc trực tiếp từ DB
        List<vn.edu.fpt.sba.music4.entities.TrackArtist> trackArtists = trackArtistRepository.findByTrackId(track.getId());

        if (trackArtists == null) {
            System.out.println("=== [TEST NOTI] LỖI: Danh sách lấy từ trackArtistRepository bị NULL! ===");
            return;
        }
        if (trackArtists.isEmpty()) {
            System.out.println("=== [TEST NOTI] LỖI: Danh sách lấy từ trackArtistRepository bị RỖNG! ===");
            return;
        }

        System.out.println("=== [TEST NOTI] 2. Số lượng nghệ sĩ tìm thấy từ DB: " + trackArtists.size());

        for (vn.edu.fpt.sba.music4.entities.TrackArtist trackArtist : trackArtists) {
            vn.edu.fpt.sba.music4.entities.Artist artist = trackArtist.getArtist();
            if (artist == null) {
                System.out.println("=== [TEST NOTI] LỖI: Đối tượng Artist bên trong TrackArtist bị NULL! ===");
                continue;
            }

            System.out.println("=== [TEST NOTI] 3. Đang xét Nghệ sĩ: " + artist.getName() + " | ID: " + artist.getId());

            List<Follow> follows = followRepository.findByArtistId(artist.getId());
            if (follows == null || follows.isEmpty()) {
                System.out.println("=== [TEST NOTI] LỖI: Không tìm thấy bất kỳ ai bấm Follow nghệ sĩ này trong DB! ===");
                continue;
            }
            System.out.println("=== [TEST NOTI] 4. Số lượng người hâm mộ sẽ nhận thông báo: " + follows.size());

            for (Follow follow : follows) {
                System.out.println("=== [TEST NOTI] Chuẩn bị lưu thông báo cho User ID: " + follow.getUser().getId());

                Notification notification = new Notification();
                notification.setUser(follow.getUser());
                notification.setArtist(artist);
                notification.setTrack(track);
                notification.setContent("Nghệ sĩ " + artist.getName() + " vừa phát hành bài hát mới: " + track.getName());
                notification.setIsRead(false);
                notification.setCreatedAt(LocalDateTime.now());

                Notification savedNoti = notificationRepository.save(notification);

                System.out.println("=== [TEST NOTI] 5. Đã lưu DB thành công! ID thông báo: " + savedNoti.getId());

                Integer targetUserId = follow.getUser().getId();
                boolean isOnline = emitters.containsKey(targetUserId);
                System.out.println("=== [TEST NOTI] Kiểm tra trạng thái online của User ID " + targetUserId + ": " + isOnline);

                if (isOnline) {
                    try {
                        // 🟢 THÊM ĐOẠN NÀY: Đưa dữ liệu sạch dạng DTO qua SSE gửi tới React để chuông nhảy số luôn
                        NotificationResponseDTO responseDTO = new NotificationResponseDTO(
                                savedNoti.getId(),
                                track.getId(),
                                track.getName(),
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
                        System.out.println("=== [TEST NOTI] Đã gọi lệnh emitter.send() gửi tới cụm React thành công! ===");
                    } catch (IOException e) {
                        System.out.println("=== [TEST NOTI] LỖI khi push real-time cho user " + targetUserId + ": " + e.getMessage());
                        emitters.remove(targetUserId);
                    }
                }
            }
        }
    }

    @Override
    public void createNewTrackNotification(Integer artistId, String artistName, Integer trackId, String trackName) {
        // Để trống vì logic đã gộp hết lên hàm trên sạch sẽ rồi
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