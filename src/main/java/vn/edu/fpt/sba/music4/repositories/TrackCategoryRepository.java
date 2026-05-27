package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.entities.TrackCategory;

@Repository
public interface TrackCategoryRepository extends JpaRepository<TrackCategory, Integer> {
}
