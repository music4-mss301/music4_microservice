package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.entities.Artist;

import java.util.List;

import java.util.Map;
import java.util.Optional;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Integer> {

    @Query(value = "SELECT COALESCE(SUM(CAST(t.view_count AS BIGINT)), 0) " +
            "FROM TRACKS t " +
            "INNER JOIN TRACK_ARTISTS ta ON t.id = ta.track_id " +
            "WHERE ta.artist_id = :artistId", nativeQuery = true)
    Long getTotalViewsByArtist(@Param("artistId") Integer artistId);

    @Query(value = "SELECT COALESCE(COUNT_BIG(f.id), 0) " +
            "FROM FAVORITES f " +
            "INNER JOIN TRACK_ARTISTS ta ON f.track_id = ta.track_id " +
            "WHERE ta.artist_id = :artistId", nativeQuery = true)
    Long getTotalFavoritesByArtist(@Param("artistId") Integer artistId);

    @Query(value = "SELECT COALESCE(COUNT_BIG(id), 0) FROM FOLLOWS WHERE artist_id = :artistId", nativeQuery = true)
    Long getTotalFollowersByArtist(@Param("artistId") Integer artistId);

    @Query(value = "SELECT COALESCE(COUNT_BIG(c.id), 0) " +
            "FROM COMMENTS c " +
            "INNER JOIN TRACKS t ON c.track_id = t.id " +
            "INNER JOIN TRACK_ARTISTS ta ON t.id = ta.track_id " +
            "WHERE ta.artist_id = :artistId", nativeQuery = true)
    Long getTotalCommentsByArtist(@Param("artistId") Integer artistId);

    Optional<Artist> findByUserName(String userName);

    @Query(value = "SELECT * FROM ARTISTS WHERE REPLACE(LOWER(name), '-', ' ') COLLATE Latin1_General_CI_AI LIKE N'%' + :keyword + '%' ORDER BY name ASC",
            countQuery = "SELECT COUNT(*) FROM ARTISTS WHERE REPLACE(LOWER(name), '-', ' ') COLLATE Latin1_General_CI_AI LIKE N'%' + :keyword + '%'",
            nativeQuery = true)
    Page<Artist> searchByName(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT a FROM Artist a WHERE a.userName = :username")
    Artist findByUsername (String username);

    
    @Query(value = "SELECT TOP 3 id, name, img, followers, favorites, trackTotal " +
            "FROM (" +
            "    SELECT a.id as id, a.name as name, a.img as img, " +
            "    (SELECT COUNT(f.id) FROM FOLLOWS f WHERE f.artist_id = a.id) as followers, " +
            "    (SELECT COUNT(fav.id) FROM FAVORITES fav JOIN TRACK_ARTISTS ta ON fav.track_id = ta.track_id WHERE ta.artist_id = a.id) as favorites, " +
            "    a.track_total as trackTotal " +
            "    FROM ARTISTS a" +
            ") AS ArtistStats " +
            "ORDER BY (followers + favorites) DESC, trackTotal DESC",
            nativeQuery = true)
    List<Map<String, Object>> getTop3Artists();

    @Query(value = """
    SELECT TOP 5
        t.id,
        t.name,
        t.view_count,
        COUNT(f.id) AS favorite_count
    FROM TRACKS t
    JOIN TRACK_ARTISTS ta
        ON ta.track_id = t.id
    LEFT JOIN FAVORITES f
        ON f.track_id = t.id
    WHERE ta.artist_id = :artistId
    GROUP BY
        t.id,
        t.name,
        t.view_count
    ORDER BY
        t.view_count DESC
    """, nativeQuery = true)
    List<Map<String, Object>> getTopTracksByArtist(@Param("artistId") Integer artistId);
}
