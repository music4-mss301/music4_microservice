package vn.edu.fpt.mss.contentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.mss.contentservice.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
}
