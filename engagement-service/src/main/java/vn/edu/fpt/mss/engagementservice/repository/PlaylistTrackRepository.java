package vn.edu.fpt.mss.engagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.mss.engagementservice.entity.PlaylistTrack;

public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, Integer> {

    boolean existsByPlaylistIdAndTrackId(Integer playlistId, Integer trackId);

    long countByPlaylistId(Integer playlistId);

    @Modifying
    @Query("DELETE FROM PlaylistTrack pt WHERE pt.playlist.id = :playlistId AND pt.trackId = :trackId")
    int deleteFromPlaylist(@Param("playlistId") Integer playlistId, @Param("trackId") Integer trackId);

    @Modifying
    @Query("DELETE FROM PlaylistTrack pt WHERE pt.trackId = :trackId")
    int deleteByTrackId(@Param("trackId") Integer trackId);
}
