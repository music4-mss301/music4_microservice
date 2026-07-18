package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.entities.Favorite;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Integer> {
    List<Favorite> findByUserId(Integer userId);

    Optional<Favorite> findByUserIdAndTrackId(Integer userId, Integer trackId);
    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);

    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.track.id IN (SELECT ta.track.id FROM TrackArtist ta WHERE ta.artist.id = :artistId AND UPPER(ta.role) = 'MAIN')")
    void deleteFavoritesByMainArtist(@Param("artistId") Integer artistId); //[cite: 53]
}