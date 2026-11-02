package vn.edu.fpt.mss.contentservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.mss.contentservice.dto.request.CategoryRequest;
import vn.edu.fpt.mss.contentservice.dto.response.CategoryDetailResponse;
import vn.edu.fpt.mss.contentservice.dto.response.CategoryResponse;
import vn.edu.fpt.mss.contentservice.entity.Category;
import vn.edu.fpt.mss.contentservice.event.EventPublisher;
import vn.edu.fpt.mss.contentservice.event.EventTypes;
import vn.edu.fpt.mss.contentservice.exception.BusinessException;
import vn.edu.fpt.mss.contentservice.exception.ResourceNotFoundException;
import vn.edu.fpt.mss.contentservice.mapper.ContentMapper;
import vn.edu.fpt.mss.contentservice.repository.CategoryRepository;
import vn.edu.fpt.mss.contentservice.repository.TrackCategoryRepository;
import vn.edu.fpt.mss.contentservice.repository.TrackRepository;
import vn.edu.fpt.mss.contentservice.service.CategoryService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final TrackCategoryRepository trackCategoryRepository;
    private final TrackRepository trackRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll(Sort.by("name")).stream().map(ContentMapper::toCategoryResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDetailResponse getDetail(Integer id) {
        Category c = find(id);
        return new CategoryDetailResponse(c.getId(), c.getName(),
                trackRepository.findByCategoryId(id).stream().map(ContentMapper::toTrackResponse).toList());
    }

    /** Rule: tên thể loại unique (không phân biệt hoa thường). */
    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        String name = req.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException("Thể loại '" + name + "' đã tồn tại");
        }
        return ContentMapper.toCategoryResponse(categoryRepository.save(Category.builder().name(name).build()));
    }

    @Override
    @Transactional
    public CategoryResponse update(Integer id, CategoryRequest req) {
        Category c = find(id);
        String name = req.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException("Thể loại '" + name + "' đã tồn tại");
        }
        c.setName(name);
        eventPublisher.publish(EventTypes.CATEGORY_UPDATED, ContentMapper.toCategoryResponse(c));
        return ContentMapper.toCategoryResponse(c);
    }

    /** Rule: không xóa thể loại đang được gán cho bài hát (409). */
    @Override
    @Transactional
    public void delete(Integer id) {
        Category c = find(id);
        long used = trackCategoryRepository.countByCategoryId(id);
        if (used > 0) {
            throw new BusinessException(String.format(
                    "Không thể xóa! Thể loại '%s' đang được dùng cho %d bài hát", c.getName(), used));
        }
        categoryRepository.delete(c);
        eventPublisher.publish(EventTypes.CATEGORY_DELETED, ContentMapper.toCategoryResponse(c));
    }

    private Category find(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thể loại id=" + id));
    }
}
