package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.dtos.response.IChartData;
import vn.edu.fpt.sba.music4.entities.History;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface HistoryRepository extends JpaRepository<History, Integer> {

    @Query(value = """
            SELECT CONVERT(varchar, h.listened_at, 23) AS label, COUNT(h.id) AS value 
            FROM HISTORY h 
            INNER JOIN TRACKS t ON h.track_id = t.id 
            INNER JOIN ALBUMS a ON t.album_id = a.id 
            WHERE a.artist_id = :artistId 
              AND h.listened_at >= DATEADD(day, -30, GETDATE())
            GROUP BY CONVERT(varchar, h.listened_at, 23)
            ORDER BY label ASC
            """, nativeQuery = true)
    List<IChartData> countViewsByArtistPerDay(@Param("artistId") Integer artistId);

    Optional<History> findTopByUserIdAndTrackIdOrderByListenedAtDesc(Integer userId, Integer trackId);

    // AI Recommendation
    List<History> findByUserId(Integer userId);

    // History pagination
    Page<History> findByUserId(Integer userId, Pageable pageable);

    @Query("SELECT h FROM History h WHERE h.id IN " +
            "(SELECT MAX(h2.id) FROM History h2 WHERE h2.user.id = :userId GROUP BY h2.track.id) " +
            "ORDER BY h.listenedAt DESC")
    Page<History> findDistinctHistoryByUserId(@Param("userId") Integer userId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM History h WHERE h.playbackPosition < 5 OR h.listenedAt < :cutoffDate")
    void cleanupGarbageHistory(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query(value = """
    SELECT
        CAST(h.listened_at AS DATE) AS day,
        COUNT(DISTINCT h.id) AS views
    FROM HISTORY h
    INNER JOIN TRACKS t
        ON h.track_id = t.id
    INNER JOIN TRACK_ARTISTS ta
        ON ta.track_id = t.id
    WHERE ta.artist_id = :artistId
      AND h.listened_at >= DATEADD(DAY, -6, CAST(GETDATE() AS DATE))
    GROUP BY CAST(h.listened_at AS DATE)
    ORDER BY day
    """, nativeQuery = true)
    List<Map<String, Object>> getDailyAnalyticsByArtist(@Param("artistId") Integer artistId);
    void deleteByUserId(Integer userId);
    @Modifying
    @Query("DELETE FROM History h WHERE h.track.id IN (SELECT ta.track.id FROM TrackArtist ta WHERE ta.artist.id = :artistId AND UPPER(ta.role) = 'MAIN')")
    void deleteHistoryByMainArtist(@Param("artistId") Integer artistId);
}
