package vn.edu.fpt.mss.contentservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.mss.contentservice.entity.Track;

import java.util.Collection;
import java.util.List;

public interface TrackRepository extends JpaRepository<Track, Integer> {

    @EntityGraph(attributePaths = "album")
    Page<Track> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = "album")
    List<Track> findByAlbumIdOrderByIdAsc(Integer albumId);

    @EntityGraph(attributePaths = "album")
    List<Track> findByIdIn(Collection<Integer> ids);

    int countByAlbumId(Integer albumId);

    boolean existsByAlbumIdAndNameIgnoreCase(Integer albumId, String name);

    boolean existsByAlbumIdAndNameIgnoreCaseAndIdNot(Integer albumId, String name, Integer id);

    @EntityGraph(attributePaths = "album")
    @Query("SELECT t FROM Track t JOIN t.trackArtists ta WHERE ta.artist.id = :artistId ORDER BY t.uploadDate DESC")
    List<Track> findByArtistId(@Param("artistId") Integer artistId);

    @Query("SELECT DISTINCT t.id FROM Track t JOIN t.trackArtists ta WHERE ta.artist.id = :artistId")
    List<Integer> findIdsByArtistId(@Param("artistId") Integer artistId);

    @EntityGraph(attributePaths = "album")
    @Query("SELECT t FROM Track t JOIN t.trackCategories tc WHERE tc.category.id = :categoryId ORDER BY t.id DESC")
    List<Track> findByCategoryId(@Param("categoryId") Integer categoryId);

    @Query("SELECT t FROM Track t ORDER BY t.viewCount DESC, t.id DESC")
    List<Track> findTrending(Pageable pageable);

    @Query("""
            SELECT DISTINCT t FROM Track t
            LEFT JOIN t.trackArtists ta LEFT JOIN ta.artist a
            WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Track> search(@Param("keyword") String keyword, Pageable pageable);

    @Modifying
    @Query("UPDATE Track t SET t.viewCount = t.viewCount + 1 WHERE t.id = :trackId")
    int incrementViewCount(@Param("trackId") Integer trackId);

    @Query("SELECT COALESCE(SUM(t.viewCount), 0) FROM Track t WHERE t.id IN "
            + "(SELECT ta.track.id FROM TrackArtist ta WHERE ta.artist.id = :artistId)")
    long sumViewsByArtist(@Param("artistId") Integer artistId);

    @Query("SELECT COALESCE(SUM(t.viewCount), 0) FROM Track t")
    long sumAllViews();
}
