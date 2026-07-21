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

@Repository
public interface TrackRepository extends JpaRepository<Track, Integer> {

    @Modifying
    @Query("UPDATE Track t SET t.viewCount = COALESCE(t.viewCount, 0) + 1 WHERE t.id = :trackId")
    void incrementViewCount(@Param("trackId") Integer trackId);

    @Query("SELECT t FROM Track t ORDER BY t.viewCount DESC")
    List<Track> findTopTrendingTracks(Pageable pageable);

    @Query("SELECT DISTINCT t FROM Track t " +
            "LEFT JOIN FETCH t.album " +
            "LEFT JOIN t.trackArtists ta " +
            "LEFT JOIN ta.artist a " +
            "WHERE (REPLACE(LOWER(t.name), '-', ' ') LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "OR (REPLACE(LOWER(a.name), '-', ' ') LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Track> searchByName(@Param("keyword") String keyword, Pageable pageable);

    List<Track> findByAlbumId(Integer albumId);

    int countByAlbumId(Integer albumId);

    List<Track> findTop5ByOrderByViewCountDesc();

    // Nếu 1 bài hát có thể có nhiều Artist collab (Many-to-Many)
    @Query("SELECT t FROM Track t JOIN t.trackArtists ta WHERE ta.artist.id = :artistId")
    List<Track> findTracksByArtistId(@Param("artistId") Integer artistId);

    @Query("SELECT DISTINCT t FROM Track t " +
            "LEFT JOIN FETCH t.album " + // Vẫn giữ Fetch Album để fix lỗi "no session" hôm nọ
            "JOIN t.trackArtists ta " +   // 👈 JOIN vào bảng trung gian trackArtists
            "WHERE ta.artist.id = :artistId")
        // 👈 Lọc ID ca sĩ từ bảng trung gian
    List<Track> findTracksByArtistIdWithAlbum(@Param("artistId") Integer artistId);

    @Query("SELECT DISTINCT t FROM Track t " +
            "LEFT JOIN FETCH t.trackArtists ta " +
            "LEFT JOIN FETCH ta.artist " +
            "WHERE t.id IN (SELECT ta2.track.id FROM TrackArtist ta2 WHERE ta2.artist.id = :artistId)")
    List<Track> findTracksByArtistIdWithAllDetails(@Param("artistId") Integer artistId);

    @Query("SELECT t FROM Track t JOIN t.trackArtists ta WHERE ta.artist.id = :artistId AND UPPER(ta.role) = 'MAIN'")
    List<Track> findMainTracksByArtistId(@Param("artistId") Integer artistId);

    @Query(value = "SELECT * FROM TRACKS ORDER BY CHECKSUM(id, :seed)",
            countQuery = "SELECT COUNT(*) FROM TRACKS",
            nativeQuery = true)
    Page<Track> findRandomTracksWithSeed(@Param("seed") int seed, Pageable pageable);

    @Query(value = "SELECT t.* FROM TRACKS t " +
            "INNER JOIN TRACK_CATEGORIES tc ON t.id = tc.track_id " +
            "WHERE tc.category_id = :categoryId AND t.is_deleted = 0",
            countQuery = "SELECT COUNT(t.id) FROM TRACKS t " +
                    "INNER JOIN TRACK_CATEGORIES tc ON t.id = tc.track_id " +
                    "WHERE tc.category_id = :categoryId AND t.is_deleted = 0",
            nativeQuery = true)
    Page<Track> findTracksByCategoryId(@Param("categoryId") Integer categoryId, Pageable pageable);

    boolean existsByImgAndIdNot(String img, Integer id);
}
