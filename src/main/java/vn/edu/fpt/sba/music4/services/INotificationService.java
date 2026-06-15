package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.dtos.response.NotificationResponseDTO;
import vn.edu.fpt.sba.music4.entities.Track;
import java.util.List;

public interface INotificationService {
    List<NotificationResponseDTO> getMyNotifications(Integer userId);
    void markAsRead(Integer notificationId);
    void sendNewTrackNotification(Track track);
}