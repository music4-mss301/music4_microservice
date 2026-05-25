package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.entities.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
