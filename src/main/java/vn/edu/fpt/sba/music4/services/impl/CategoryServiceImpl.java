package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.sba.music4.dtos.response.CategoryDetailResponseDTO;
import vn.edu.fpt.sba.music4.dtos.response.CategoryResponseDTO;
import vn.edu.fpt.sba.music4.dtos.response.TrackResponseDTO;
import vn.edu.fpt.sba.music4.entities.Category;
import vn.edu.fpt.sba.music4.entities.Track;
import vn.edu.fpt.sba.music4.exceptions.BusinessException;
import vn.edu.fpt.sba.music4.repositories.CategoryRepository;
import vn.edu.fpt.sba.music4.repositories.TrackCategoryRepository;
import vn.edu.fpt.sba.music4.services.ICategoryService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements ICategoryService {
    private CategoryRepository categoryRepository;
    private TrackCategoryRepository trackCategoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository, TrackCategoryRepository trackCategoryRepository) {
        this.categoryRepository = categoryRepository;
        this.trackCategoryRepository = trackCategoryRepository;
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
    public CategoryDetailResponseDTO getCategoryById(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thể loại với ID: " + id));
        List<TrackResponseDTO> trackDtos = category.getTrackCategories().stream()
                .map(trackCategory -> {
                    Track track = trackCategory.getTrack();
                    // Map thủ công hoặc dùng Mapper để biến đổi từ Track sang TrackResponseDTO
                    return new TrackResponseDTO(
                            track.getId(),
                            track.getName(),
                            track.getAlbum().getName(),
                            track.getImg(),
                            track.getDuration(),
                            track.getFilePath(),
                            track.getPreviewPath(),
                            track.getViewCount(),
                            track.getUploadDate(),
                            null
                    );
                })
                .collect(Collectors.toList());
        return new CategoryDetailResponseDTO(category.getId(), category.getName(), trackDtos);
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
    public CategoryResponseDTO updateCategory(Integer id, Category categoryDetails) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thể loại ID: " + id));

        if (categoryDetails.getName() == null || categoryDetails.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên thể loại không được để trống!");
        }

        existingCategory.setName(categoryDetails.getName().trim());
        Category savedCategory = categoryRepository.save(existingCategory);

        return new CategoryResponseDTO(savedCategory.getId(), savedCategory.getName());
    }

    @Override
    @Transactional
    public void deleteCategory(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thể loại ID: " + id));

        long trackCount = trackCategoryRepository.countByCategoryId(id);

        if (trackCount > 0) {
            throw new BusinessException(
                    String.format("Không thể xóa! Thể loại '%s' đang được áp dụng cho %d bài hát. " +
                                    "Vui lòng chuyển các bài hát này sang thể loại khác trước khi thực hiện xóa.",
                            category.getName(), trackCount)
            );
        }

        categoryRepository.delete(category);
    }

    @Override
    public List<Map<String, Object>> getCategoryStats() {
        return categoryRepository.getCategoryStats();
    }
}