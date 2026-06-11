package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.dtos.response.CategoryDetailResponseDTO;
import vn.edu.fpt.sba.music4.dtos.response.CategoryResponseDTO;
import vn.edu.fpt.sba.music4.entities.Category;

import java.util.List;

public interface ICategoryService {
    List<CategoryResponseDTO> getAllCategories();
    CategoryDetailResponseDTO getCategoryById(Integer id);
    Category createCategory(Category category);
    CategoryResponseDTO updateCategory(Integer id, Category category);
    void deleteCategory(Integer id);
}

