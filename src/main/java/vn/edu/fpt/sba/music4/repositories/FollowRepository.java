package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.entities.Follow;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Integer> {

    Optional<Follow> findByUserIdAndArtistId(Integer userId, Integer artistId);

    List<Follow> findByArtistId(Integer artistId);

    List<Follow> findByUserId(Integer userId);

    @Query("SELECT f FROM Follow f JOIN FETCH f.user WHERE f.artist.id = :artistId")
    List<Follow> findFollowersByArtistId(@Param("artistId") Integer artistId);
}