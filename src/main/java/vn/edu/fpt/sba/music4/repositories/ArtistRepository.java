package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.entities.Artist;

import java.util.List;

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

    Optional<Artist> findByUserName(String userName);

    @Query(value = "SELECT * FROM ARTISTS WHERE REPLACE(LOWER(name), '-', ' ') COLLATE Latin1_General_CI_AI LIKE N'%' + :keyword + '%' ORDER BY name ASC",
            countQuery = "SELECT COUNT(*) FROM ARTISTS WHERE REPLACE(LOWER(name), '-', ' ') COLLATE Latin1_General_CI_AI LIKE N'%' + :keyword + '%'",
            nativeQuery = true)
    Page<Artist> searchByName(@Param("keyword") String keyword, Pageable pageable);
}
