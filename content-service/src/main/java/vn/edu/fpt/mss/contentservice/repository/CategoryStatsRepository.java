package vn.edu.fpt.mss.contentservice.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import vn.edu.fpt.mss.contentservice.entity.Category;

import java.util.List;

public interface CategoryStatsRepository extends Repository<Category, Integer> {

    /** Mỗi dòng: [id, name, số bài hát]. Thể loại chưa có bài vẫn xuất hiện với 0. */
    @Query("""
            SELECT c.id, c.name, COUNT(tc.id) FROM Category c
            LEFT JOIN TrackCategory tc ON tc.category.id = c.id
            GROUP BY c.id, c.name ORDER BY COUNT(tc.id) DESC, c.name ASC
            """)
    List<Object[]> stats();
}
