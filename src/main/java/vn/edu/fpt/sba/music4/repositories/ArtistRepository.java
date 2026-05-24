package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.entities.Artist;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Integer> {

    // Tính tổng lượt nghe của tất cả bài hát thuộc về 1 Artist
    @Query(value = "SELECT ISNULL(SUM(t.view_count), 0) FROM TRACKS t INNER JOIN ALBUMS a ON t.album_id = a.id WHERE a.artist_id = :artistId", nativeQuery = true)
    Long getTotalViewsByArtist(@Param("artistId") Integer artistId);

    // Tính tổng số lượt thả tim (Favorites) cho nhạc của Artist
    @Query(value = "SELECT COUNT(f.id) FROM FAVORITES f INNER JOIN TRACKS t ON f.track_id = t.id INNER JOIN ALBUMS a ON t.album_id = a.id WHERE a.artist_id = :artistId", nativeQuery = true)
    Long getTotalFavoritesByArtist(@Param("artistId") Integer artistId);

    // Tính tổng số người theo dõi Artist
    @Query(value = "SELECT COUNT(id) FROM FOLLOWS WHERE artist_id = :artistId", nativeQuery = true)
    Long getTotalFollowersByArtist(@Param("artistId") Integer artistId);
}
