package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.entities.Notification;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Integer userId);
    List<Notification> findByAlbumId(Integer albumId);
    void deleteByUserId(Integer userId);
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.artist.id = :artistId OR n.track.id IN (SELECT ta.track.id FROM TrackArtist ta WHERE ta.artist.id = :artistId AND UPPER(ta.role) = 'MAIN')")
    void deleteNotificationsByArtistOrMainTrack(@Param("artistId") Integer artistId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.album.id = :albumId")
    void deleteByAlbumId(@Param("albumId") Integer albumId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.track.id = :trackId")
    void deleteByTrackId(@Param("trackId") Integer trackId);
}