package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.dtos.response.IChartData;
import vn.edu.fpt.sba.music4.entities.History;

import java.util.List;
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

    // Get the most recent listening record of a User for a specific Track
    Optional<History> findTopByUserIdAndTrackIdOrderByListenedAtDesc(Integer userId, Integer trackId);

    // Use for AI Recommendation
    List<History> findByUserId(Integer userId);

    // Use for History pagination
    Page<History> findByUserId(Integer userId, Pageable pageable);
}
