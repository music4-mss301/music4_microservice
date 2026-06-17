package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.entities.Track;

import java.util.List;
import java.util.Map;

@Repository
public interface TrackRepository extends JpaRepository<Track, Integer> {

    @Modifying
    @Query("UPDATE Track t SET t.viewCount = COALESCE(t.viewCount, 0) + 1 WHERE t.id = :trackId")
    void incrementViewCount(@Param("trackId") Integer trackId);

    @Query("SELECT t FROM Track t ORDER BY t.viewCount DESC")
    List<Track> findTopTrendingTracks(Pageable pageable);

    @Query(value = "SELECT DISTINCT t.* FROM TRACKS t " +
            "LEFT JOIN Track_Artists ta ON t.id = ta.track_id " +
            "LEFT JOIN ARTISTS a ON ta.artist_id = a.id " +
            "WHERE (REPLACE(LOWER(t.name), '-', ' ') COLLATE Latin1_General_CI_AI LIKE N'%' + :keyword + '%') " +
            "OR (REPLACE(LOWER(a.name), '-', ' ') COLLATE Latin1_General_CI_AI LIKE N'%' + :keyword + '%') " +
            "ORDER BY t.name ASC",
            countQuery = "SELECT COUNT(DISTINCT t.id) FROM TRACKS t " +
                    "LEFT JOIN Track_Artists ta ON t.id = ta.track_id " +
                    "LEFT JOIN ARTISTS a ON ta.artist_id = a.id " +
                    "WHERE (REPLACE(LOWER(t.name), '-', ' ') COLLATE Latin1_General_CI_AI LIKE N'%' + :keyword + '%') " +
                    "OR (REPLACE(LOWER(a.name), '-', ' ') COLLATE Latin1_General_CI_AI LIKE N'%' + :keyword + '%')",
            nativeQuery = true)
    Page<Track> searchByName(@Param("keyword") String keyword, Pageable pageable);
    List<Track> findByAlbumId(Integer albumId);
    int countByAlbumId(Integer albumId);
    List<Track> findTop5ByOrderByViewCountDesc();

}
