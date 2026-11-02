package vn.edu.fpt.mss.engagementservice.service;

import vn.edu.fpt.mss.engagementservice.dto.event.AlbumEventPayload;
import vn.edu.fpt.mss.engagementservice.dto.event.TrackEventPayload;
import vn.edu.fpt.mss.engagementservice.dto.response.NotificationResponse;
import vn.edu.fpt.mss.engagementservice.dto.response.PageResponse;

import java.util.List;

public interface NotificationService {
    PageResponse<NotificationResponse> getOf(Integer userId, int page, int size);

    /** FE dùng GET /notifications/user/{id} và mong đợi một MẢNG (res.data.filter). */
    List<NotificationResponse> listOf(Integer userId);

    long unreadCount();

    NotificationResponse markRead(Integer id);

    int markAllRead();

    void delete(Integer id);

    int clearAll();

    int notifyFollowersOfNewTrack(TrackEventPayload track);

    int notifyFollowersOfNewAlbum(AlbumEventPayload album);
}
