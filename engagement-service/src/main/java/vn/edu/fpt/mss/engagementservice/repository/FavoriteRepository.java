package vn.edu.fpt.mss.engagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.mss.engagementservice.entity.Favorite;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Integer> {

    Optional<Favorite> findByUserIdAndTrackId(Integer userId, Integer trackId);

    boolean existsByUserIdAndTrackId(Integer userId, Integer trackId);

    long countByTrackId(Integer trackId);

    long countByTrackIdIn(Collection<Integer> trackIds);

    @Modifying
    @Query("DELETE FROM Favorite x WHERE x.trackId = :trackId")
    int deleteByTrackId(@Param("trackId") Integer trackId);

    @Modifying
    @Query("DELETE FROM Favorite x WHERE x.userId = :userId")
    int deleteByUserId(@Param("userId") Integer userId);

    List<Favorite> findByUserIdOrderByLikedAtDesc(Integer userId);
}
