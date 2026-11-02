package vn.edu.fpt.mss.engagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.mss.engagementservice.entity.Follow;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Integer> {

    Optional<Follow> findByUserIdAndArtistId(Integer userId, Integer artistId);

    boolean existsByUserIdAndArtistId(Integer userId, Integer artistId);

    long countByArtistId(Integer artistId);

    List<Follow> findByUserIdOrderByFollowedAtDesc(Integer userId);

    @Query("SELECT DISTINCT f.userId FROM Follow f WHERE f.artistId IN :artistIds")
    List<Integer> findFollowerIds(@Param("artistIds") Collection<Integer> artistIds);

    @Modifying
    @Query("DELETE FROM Follow f WHERE f.artistId = :artistId")
    int deleteByArtistId(@Param("artistId") Integer artistId);

    @Modifying
    @Query("DELETE FROM Follow f WHERE f.userId = :userId")
    int deleteByUserId(@Param("userId") Integer userId);
}
