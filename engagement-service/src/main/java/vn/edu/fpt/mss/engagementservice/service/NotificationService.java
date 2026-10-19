package vn.edu.fpt.mss.engagementservice.service;

import vn.edu.fpt.mss.engagementservice.dto.event.TrackEventPayload;
import vn.edu.fpt.mss.engagementservice.dto.response.NotificationResponse;
import vn.edu.fpt.mss.engagementservice.dto.response.PageResponse;

public interface NotificationService {
    PageResponse<NotificationResponse> getOf(Integer userId, int page, int size);

    long unreadCount();

    NotificationResponse markRead(Integer id);

    int markAllRead();

    void delete(Integer id);

    int notifyFollowersOfNewTrack(TrackEventPayload track);
}
