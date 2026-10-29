package vn.edu.fpt.mss.contentservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.fpt.mss.contentservice.dto.request.CategoryRequest;
import vn.edu.fpt.mss.contentservice.entity.Category;
import vn.edu.fpt.mss.contentservice.event.EventPublisher;
import vn.edu.fpt.mss.contentservice.exception.BusinessException;
import vn.edu.fpt.mss.contentservice.repository.CategoryRepository;
import vn.edu.fpt.mss.contentservice.repository.TrackCategoryRepository;
import vn.edu.fpt.mss.contentservice.repository.TrackRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    TrackCategoryRepository trackCategoryRepository;

    @Mock
    TrackRepository trackRepository;

    @Mock
    EventPublisher eventPublisher;

    @InjectMocks
    CategoryServiceImpl categoryService;

    /** Rule: Tên thể loại unique (không phân biệt hoa thường) -> vi phạm ném 409 BusinessException */
    @Test
    void create_whenNameExists_throwsBusinessException() {
        when(categoryRepository.existsByNameIgnoreCase("Pop")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> categoryService.create(new CategoryRequest("Pop")));

        verify(categoryRepository, never()).save(any());
    }

    /** Rule: Không xóa thể loại đang được gán cho bài hát -> vi phạm ném 409 BusinessException */
    @Test
    void delete_whenCategoryHasTracks_throwsBusinessException() {
        Category pop = Category.builder().id(1).name("Pop").build();
        when(categoryRepository.findById(1)).thenReturn(Optional.of(pop));
        when(trackCategoryRepository.countByCategoryId(1)).thenReturn(3L);

        assertThrows(BusinessException.class, () -> categoryService.delete(1));

        verify(categoryRepository, never()).delete(any());
    }

    /** Khi tên thể loại chưa tồn tại -> lưu thành công */
    @Test
    void create_whenNameIsUnique_savesSuccessfully() {
        when(categoryRepository.existsByNameIgnoreCase("Indie")).thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenReturn(Category.builder().id(11).name("Indie").build());

        var response = categoryService.create(new CategoryRequest("Indie"));

        assertEquals(11, response.id());
        assertEquals("Indie", response.name());
        verify(categoryRepository).save(any(Category.class));
    }
}