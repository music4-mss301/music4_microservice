package vn.edu.fpt.sba.music4.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.response.CategoryResponseDTO;
import vn.edu.fpt.sba.music4.entities.Category;
import vn.edu.fpt.sba.music4.services.ICategoryService;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Thể loại (Category API)", description = "Quản lý danh mục thể loại nhạc phục vụ Admin")
public class CategoryController {
    private final ICategoryService categoryService;

    public CategoryController(ICategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "Lấy toàn bộ danh sách thể loại nhạc", description = "Trả về tất cả thể loại có trong hệ thống để đưa vào form chọn")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Tìm thể loại theo ID", description = "Trả về chi tiết một thể loại")
    @ApiResponse(responseCode = "200", description = "Tìm thấy thành công")
    public ResponseEntity<Category> getCategoryById(
            @Parameter(description = "ID của thể loại", example = "1") @PathVariable Integer id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PostMapping
    @Operation(summary = "Thêm mới một thể loại nhạc", description = "Tạo một dòng thể loại mới (Admin)")
    @ApiResponse(responseCode = "200", description = "Thêm mới thành công")
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        return ResponseEntity.ok(categoryService.createCategory(category));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật tên thể loại", description = "Chỉnh sửa tên hiển thị của thể loại nhạc")
    @ApiResponse(responseCode = "200", description = "Cập nhật thành công")
    public ResponseEntity<Category> updateCategory(
            @Parameter(description = "ID của thể loại cần sửa", example = "1") @PathVariable Integer id,
            @RequestBody Category categoryDetails) {
        return ResponseEntity.ok(categoryService.updateCategory(id, categoryDetails));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa thể loại nhạc", description = "Xóa thể loại nhạc khỏi cơ sở dữ liệu")
    @ApiResponse(responseCode = "200", description = "Xóa thành công")
    public ResponseEntity<String> deleteCategory(
            @Parameter(description = "ID của thể loại cần xóa", example = "1") @PathVariable Integer id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok("Xóa thể loại thành công!");
    }

}
