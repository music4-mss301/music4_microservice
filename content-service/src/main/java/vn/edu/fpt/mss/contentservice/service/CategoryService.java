package vn.edu.fpt.mss.contentservice.service;

import vn.edu.fpt.mss.contentservice.dto.request.CategoryRequest;
import vn.edu.fpt.mss.contentservice.dto.response.CategoryDetailResponse;
import vn.edu.fpt.mss.contentservice.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAll();

    CategoryDetailResponse getDetail(Integer id);

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Integer id, CategoryRequest request);

    void delete(Integer id);
}
