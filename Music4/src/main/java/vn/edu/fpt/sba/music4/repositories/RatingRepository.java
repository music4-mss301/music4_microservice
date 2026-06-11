package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.entities.Rating;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Integer> {

    Optional<Rating> findByUserIdAndTrackId(Integer userId, Integer trackId);

    List<Rating> findByUserId(Integer userId);
}
