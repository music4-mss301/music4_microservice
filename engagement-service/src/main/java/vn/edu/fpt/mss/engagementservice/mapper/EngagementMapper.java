package vn.edu.fpt.mss.engagementservice.mapper;

import vn.edu.fpt.mss.engagementservice.dto.client.UserSummary;
import vn.edu.fpt.mss.engagementservice.dto.response.CommentResponse;
import vn.edu.fpt.mss.engagementservice.dto.response.NotificationResponse;
import vn.edu.fpt.mss.engagementservice.dto.response.PlaylistResponse;
import vn.edu.fpt.mss.engagementservice.entity.Comment;
import vn.edu.fpt.mss.engagementservice.entity.Notification;
import vn.edu.fpt.mss.engagementservice.entity.Playlist;

public final class EngagementMapper {

    private EngagementMapper() {
    }

    public static PlaylistResponse toPlaylistResponse(Playlist p) {
        return new PlaylistResponse(p.getId(), p.getName(), p.getDescription(), p.getImg(), p.isPublic(),
                p.getUserId(), p.getTracks().size(), p.getCreatedAt());
    }

    public static CommentResponse toCommentResponse(Comment c, UserSummary user) {
        boolean edited = c.getUpdatedAt() != null && c.getCreatedAt() != null
                && c.getUpdatedAt().isAfter(c.getCreatedAt().plusSeconds(1));
        return new CommentResponse(c.getId(), c.getTrackId(), c.getUserId(),
                user == null ? "Người dùng" : user.name(), user == null ? null : user.img(),
                c.getContent(), c.getCreatedAt(), edited);
    }

    public static NotificationResponse toNotificationResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getContent(), n.getTrackId(), n.getArtistId(), n.isRead(),
                n.getCreatedAt());
    }
}
