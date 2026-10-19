package vn.edu.fpt.mss.engagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.mss.engagementservice.entity.Rating;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Integer> {

    Optional<Rating> findByUserIdAndTrackId(Integer userId, Integer trackId);

    boolean existsByUserIdAndTrackId(Integer userId, Integer trackId);

    long countByTrackId(Integer trackId);

    long countByTrackIdIn(Collection<Integer> trackIds);

    @Modifying
    @Query("DELETE FROM Rating x WHERE x.trackId = :trackId")
    int deleteByTrackId(@Param("trackId") Integer trackId);

    @Modifying
    @Query("DELETE FROM Rating x WHERE x.userId = :userId")
    int deleteByUserId(@Param("userId") Integer userId);

    List<Rating> findByUserId(Integer userId);

    @Query("SELECT COALESCE(AVG(CAST(r.ratingValue AS double)), 0) FROM Rating r WHERE r.trackId = :trackId")
    double averageOf(@Param("trackId") Integer trackId);
}
