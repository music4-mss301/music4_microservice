package vn.edu.fpt.sba.music4.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.response.CategoryDetailResponseDTO;
import vn.edu.fpt.sba.music4.dtos.response.CategoryResponseDTO;
import vn.edu.fpt.sba.music4.dtos.response.TrackResponseDTO;
import vn.edu.fpt.sba.music4.entities.Category;
import vn.edu.fpt.sba.music4.exceptions.BusinessException;
import vn.edu.fpt.sba.music4.services.ICategoryService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Thể loại (Category API)", description = "Quản lý danh mục thể loại nhạc phục vụ Admin")
public class CategoryController {
    private final ICategoryService categoryService;

    public CategoryController(ICategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // ok
    @GetMapping("")
    @Operation(summary = "Lấy toàn bộ danh sách thể loại nhạc", description = "Trả về tất cả thể loại có trong hệ thống để đưa vào form chọn")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    // ok
    @GetMapping("/{id}")
    @Operation(summary = "Tìm thể loại theo ID", description = "Trả về chi tiết một thể loại bao gồm các bài hát thuộc thể loại đó")
    @ApiResponse(responseCode = "200", description = "Tìm thấy thành công")
    public ResponseEntity<CategoryDetailResponseDTO> getCategoryById(
            @Parameter(description = "ID của thể loại", example = "1") @PathVariable Integer id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    // ok
    @PostMapping("")
    @Operation(summary = "Thêm mới một thể loại nhạc", description = "Tạo một dòng thể loại mới (Admin)")
    @ApiResponse(responseCode = "200", description = "Thêm mới thành công")
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        return ResponseEntity.ok(categoryService.createCategory(category));
    }

    // ok
    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật tên thể loại", description = "Chỉnh sửa tên hiển thị của thể loại nhạc")
    @ApiResponse(responseCode = "200", description = "Cập nhật thành công")
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            @Parameter(description = "ID của thể loại cần sửa", example = "1") @PathVariable Integer id,
            @RequestBody Category categoryDetails) {
        return ResponseEntity.ok(categoryService.updateCategory(id, categoryDetails));
    }

    // ok
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa thể loại nhạc", description = "Xóa thể loại nhạc khỏi cơ sở dữ liệu")
    @ApiResponse(responseCode = "200", description = "Xóa thành công")
    public ResponseEntity<String> deleteCategory(
            @Parameter(description = "ID của thể loại cần xóa", example = "1") @PathVariable Integer id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok("Xóa thể loại thành công!");
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
    @GetMapping("/stats")
    public ResponseEntity<List<Map<String, Object>>> getCategoryStats() {
        // Giả định bạn đã tạo hàm getCategoryStats() trong ICategoryService
        return ResponseEntity.ok(categoryService.getCategoryStats());
    }
    @GetMapping("/{id}/tracks")
    public ResponseEntity<Page<TrackResponseDTO>> getTracksByCategory(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(categoryService.getTracksByCategory(id, pageable));
    }
}
