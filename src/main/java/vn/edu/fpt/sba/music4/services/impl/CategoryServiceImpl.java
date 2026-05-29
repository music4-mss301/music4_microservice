package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.sba.music4.dtos.response.CategoryResponseDTO;
import vn.edu.fpt.sba.music4.entities.Category;
import vn.edu.fpt.sba.music4.repositories.CategoryRepository;
import vn.edu.fpt.sba.music4.services.ICategoryService;

import java.util.List;

@Service
public class CategoryServiceImpl implements ICategoryService {
    private CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryResponseDTO toResponseDto(Category category) {
        if (category == null) return null;

        return new CategoryResponseDTO(
                category.getId(),
                category.getName()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Category getCategoryById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thể loại với ID: " + id));
    }

    @Override
    @Transactional
    public Category createCategory(Category category) {
        // Đảm bảo tên thể loại không bị trống
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên thể loại không được để trống!");
        }
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category updateCategory(Integer id, Category categoryDetails) {
        Category existingCategory = getCategoryById(id);

        if (categoryDetails.getName() == null || categoryDetails.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên thể loại không được để trống!");
        }

        existingCategory.setName(categoryDetails.getName().trim());
        return categoryRepository.save(existingCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Integer id) {
        Category category = getCategoryById(id);
        categoryRepository.delete(category);
    }
}