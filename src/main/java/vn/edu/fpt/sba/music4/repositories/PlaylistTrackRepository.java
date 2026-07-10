package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.sba.music4.entities.PlaylistTrack;

import java.util.List;
import java.util.Optional;

public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, Integer> {
    Optional<PlaylistTrack> findByPlaylistIdAndTrackId(Integer playlistId, Integer trackId);

    @Query("SELECT pt FROM PlaylistTrack pt JOIN FETCH pt.track t LEFT JOIN FETCH t.album WHERE pt.playlist.id = :playlistId")
    List<PlaylistTrack> findByPlaylistId(@Param("playlistId") Integer playlistId);

    boolean existsByPlaylistIdAndTrackId(Integer playlistId, Integer trackId);
}
