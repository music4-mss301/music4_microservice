package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.dtos.response.NotificationResponseDTO;
import vn.edu.fpt.sba.music4.entities.Album;
import vn.edu.fpt.sba.music4.entities.Notification;
import vn.edu.fpt.sba.music4.entities.Track;
import java.util.List;

public interface INotificationService {
    List<NotificationResponseDTO> getMyNotifications(Integer userId);
    void markAsRead(Integer notificationId);
    void sendNewTrackNotification(Track track);
    void sendNewAlbumNotification(Album album);
    void createNewTrackNotification(Integer artistId, String artistName, Integer trackId, String trackName);
    void deleteNotification(Integer notificationId);
    void deleteAllMyNotifications(Integer userId);
    void saveAndSend(Notification notification);
}