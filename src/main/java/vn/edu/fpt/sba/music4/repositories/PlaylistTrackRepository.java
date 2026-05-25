package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.sba.music4.entities.PlaylistTrack;

import java.util.Optional;

public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, Integer> {
    Optional<PlaylistTrack> findByPlaylistIdAndTrackId(Integer playlistId, Integer trackId);

    boolean existsByPlaylistIdAndTrackId(Integer playlistId, Integer trackId);
}
