package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.entities.Track;

import java.util.List;

@Repository
public interface TrackRepository extends JpaRepository<Track, Integer> {

    @Modifying
    @Query("UPDATE Track t SET t.viewCount = COALESCE(t.viewCount, 0) + 1 WHERE t.id = :trackId")
    void incrementViewCount(@Param("trackId") Integer trackId);

    @Query("SELECT t FROM Track t ORDER BY t.viewCount DESC")
    List<Track> findTopTrendingTracks(Pageable pageable);
}
