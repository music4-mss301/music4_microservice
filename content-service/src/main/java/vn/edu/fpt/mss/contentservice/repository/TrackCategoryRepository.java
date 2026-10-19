package vn.edu.fpt.mss.contentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.mss.contentservice.entity.TrackCategory;

public interface TrackCategoryRepository extends JpaRepository<TrackCategory, Integer> {

    long countByCategoryId(Integer categoryId);
}
