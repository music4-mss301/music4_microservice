package vn.edu.fpt.mss.analyticsservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.mss.analyticsservice.dto.response.DailyCount;
import vn.edu.fpt.mss.analyticsservice.dto.response.TrackPlayCount;
import vn.edu.fpt.mss.analyticsservice.entity.History;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HistoryRepository extends JpaRepository<History, Integer> {

    boolean existsByUserIdAndTrackIdAndListenedAtAfter(Integer userId, Integer trackId, LocalDateTime after);

    Optional<History> findTopByUserIdAndTrackIdOrderByListenedAtDesc(Integer userId, Integer trackId);

    Page<History> findByUserIdOrderByListenedAtDesc(Integer userId, Pageable pageable);

    @Query("SELECT h.trackId FROM History h WHERE h.userId = :userId GROUP BY h.trackId ORDER BY MAX(h.listenedAt) DESC")
    List<Integer> findRecentTrackIds(@Param("userId") Integer userId, Pageable pageable);

    long countByListenedAtAfter(LocalDateTime after);

    @Query("SELECT COUNT(h) FROM History h WHERE h.trackId IN :trackIds AND h.listenedAt >= :from")
    long countPlaysOf(@Param("trackIds") Collection<Integer> trackIds, @Param("from") LocalDateTime from);

    @Query("""
            SELECT new vn.edu.fpt.mss.analyticsservice.dto.response.DailyCount(CAST(h.listenedAt AS LocalDate), COUNT(h))
            FROM History h WHERE h.trackId IN :trackIds AND h.listenedAt >= :from
            GROUP BY CAST(h.listenedAt AS LocalDate) ORDER BY CAST(h.listenedAt AS LocalDate)
            """)
    List<DailyCount> dailyPlaysOf(@Param("trackIds") Collection<Integer> trackIds, @Param("from") LocalDateTime from);

    @Query("""
            SELECT new vn.edu.fpt.mss.analyticsservice.dto.response.DailyCount(CAST(h.listenedAt AS LocalDate), COUNT(h))
            FROM History h WHERE h.listenedAt >= :from
            GROUP BY CAST(h.listenedAt AS LocalDate) ORDER BY CAST(h.listenedAt AS LocalDate)
            """)
    List<DailyCount> dailyPlays(@Param("from") LocalDateTime from);

    @Query("""
            SELECT new vn.edu.fpt.mss.analyticsservice.dto.response.TrackPlayCount(h.trackId, COUNT(h))
            FROM History h WHERE h.listenedAt >= :from GROUP BY h.trackId ORDER BY COUNT(h) DESC
            """)
    List<TrackPlayCount> topTracks(@Param("from") LocalDateTime from, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT h.userId) FROM History h WHERE h.userId IS NOT NULL AND h.listenedAt >= :from")
    long countDistinctListeners(@Param("from") LocalDateTime from);

    @Modifying
    @Query("DELETE FROM History h WHERE h.userId = :userId")
    int deleteByUserId(@Param("userId") Integer userId);

    @Modifying
    @Query("DELETE FROM History h WHERE h.listenedAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);

    @Modifying
    @Query("UPDATE History h SET h.userId = NULL WHERE h.userId = :userId")
    int anonymize(@Param("userId") Integer userId);
}
