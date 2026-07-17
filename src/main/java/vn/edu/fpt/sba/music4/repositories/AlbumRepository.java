package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.entities.Album;
import vn.edu.fpt.sba.music4.entities.Artist;

import java.util.List;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Integer> {
    @Query("SELECT a FROM Album a WHERE a.artist.id = :artistId")
    List<Album> findByArtistId(@Param("artistId") Integer artistId);
    @Query(value = "SELECT al.* FROM ALBUMS al " +
            "LEFT JOIN ARTISTS ar ON al.artist_id = ar.id " +
            "WHERE (REPLACE(LOWER(al.name), '-', ' ') COLLATE Latin1_General_CI_AI LIKE N'%' + :keyword + '%') " +
            "OR (REPLACE(LOWER(ar.name), '-', ' ') COLLATE Latin1_General_CI_AI LIKE N'%' + :keyword + '%') " +
            "ORDER BY al.name ASC",
            countQuery = "SELECT COUNT(*) FROM ALBUMS al " +
                    "LEFT JOIN ARTISTS ar ON al.artist_id = ar.id " +
                    "WHERE (REPLACE(LOWER(al.name), '-', ' ') COLLATE Latin1_General_CI_AI LIKE N'%' + :keyword + '%') " +
                    "OR (REPLACE(LOWER(ar.name), '-', ' ') COLLATE Latin1_General_CI_AI LIKE N'%' + :keyword + '%')",
            nativeQuery = true)
    Page<Album> searchByName(@Param("keyword") String keyword, Pageable pageable);
    int countByArtistId(Integer artistId);

    @Query(value = "SELECT a FROM Album a JOIN FETCH a.artist",
            countQuery = "SELECT COUNT(a) FROM Album a")
    Page<Album> findAllWithArtist(Pageable pageable);


}
