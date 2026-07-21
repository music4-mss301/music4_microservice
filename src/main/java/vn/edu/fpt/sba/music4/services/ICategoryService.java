package vn.edu.fpt.sba.music4.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import vn.edu.fpt.sba.music4.dtos.response.CategoryDetailResponseDTO;
import vn.edu.fpt.sba.music4.dtos.response.CategoryResponseDTO;
import vn.edu.fpt.sba.music4.dtos.response.TrackResponseDTO;
import vn.edu.fpt.sba.music4.entities.Category;

import java.util.List;
import java.util.Map;

public interface ICategoryService {
    List<CategoryResponseDTO> getAllCategories();
    CategoryDetailResponseDTO getCategoryById(Integer id);
    Category createCategory(Category category);
    CategoryResponseDTO updateCategory(Integer id, Category category);
    void deleteCategory(Integer id);

    List<Map<String, Object>> getCategoryStats();
    Page<TrackResponseDTO> getTracksByCategory(Integer categoryId, Pageable pageable);

}

