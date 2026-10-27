package vn.edu.fpt.mss.contentservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.mss.contentservice.entity.Track;

public interface TrackByCategoryRepository extends Repository<Track, Integer> {

    @EntityGraph(attributePaths = "album")
    @Query(value = "SELECT t FROM Track t JOIN t.trackCategories tc WHERE tc.category.id = :categoryId ORDER BY t.id DESC",
            countQuery = "SELECT COUNT(t) FROM Track t JOIN t.trackCategories tc WHERE tc.category.id = :categoryId")
    Page<Track> findByCategoryId(@Param("categoryId") Integer categoryId, Pageable pageable);
}
