package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.entities.Category;
import vn.edu.fpt.sba.music4.entities.Playlist;

import java.util.List;
import java.util.Map;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    @Query(value = "SELECT * FROM Categories WHERE REPLACE(LOWER(name), '-', ' ') COLLATE Latin1_General_CI_AI LIKE N'%' + :keyword + '%' ORDER BY name ASC",
            countQuery = "SELECT COUNT(*) FROM Categories WHERE REPLACE(LOWER(name), '-', ' ') COLLATE Latin1_General_CI_AI LIKE N'%' + :keyword + '%'",
            nativeQuery = true)
    Page<Category> searchByName(@Param("keyword") String keyword, Pageable pageable);


    @Query(value = "SELECT c.name AS name, COUNT(tc.track_id) AS value " +
            "FROM TRACK_CATEGORIES tc " +
            "INNER JOIN CATEGORIES c ON tc.category_id = c.id " +
            "GROUP BY c.name",
            nativeQuery = true)
    List<Map<String, Object>> getCategoryStats();
    boolean existsByNameIgnoreCase(String name);
}

