package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.entities.Follow;
import vn.edu.fpt.sba.music4.entities.PlaylistTrack;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Integer> {

    Optional<Follow> findByUserIdAndArtistId(Integer userId, Integer artistId);

    List<Follow> findByArtistId(Integer artistId);

    List<Follow> findByUserId(Integer userId);

    @Query("DELETE FROM Follow f WHERE f.artist.id = :artistId")
    Void deleteFollowers(@Param("artistId") Integer artistId);




    @Query("SELECT f FROM Follow f JOIN FETCH f.user WHERE f.artist.id = :artistId")
    List<Follow> findFollowersByArtistId(@Param("artistId") Integer artistId);

    @Modifying
    @Query("DELETE FROM Follow f WHERE f.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);


}