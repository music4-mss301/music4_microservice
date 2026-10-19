package vn.edu.fpt.mss.userservice.event;

/** Routing key thống nhất toàn hệ thống: <domain>.<entity>.<action>. */
public final class EventTypes {
    public static final String TRACK_CREATED = "content.track.created";
    public static final String TRACK_UPDATED = "content.track.updated";
    public static final String TRACK_DELETED = "content.track.deleted";
    public static final String ALBUM_CREATED = "content.album.created";
    public static final String ALBUM_UPDATED = "content.album.updated";
    public static final String ALBUM_DELETED = "content.album.deleted";
    public static final String ARTIST_CREATED = "content.artist.created";
    public static final String ARTIST_UPDATED = "content.artist.updated";
    public static final String ARTIST_DELETED = "content.artist.deleted";
    public static final String CATEGORY_UPDATED = "content.category.updated";
    public static final String CATEGORY_DELETED = "content.category.deleted";
    public static final String TRACK_PLAYED = "analytics.track.played";
    public static final String USER_LOCKED = "user.account.locked";
    public static final String USER_DELETED = "user.account.deleted";
    public static final String USER_PROFILE_UPDATED = "user.profile.updated";

    private EventTypes() {
    }
}
