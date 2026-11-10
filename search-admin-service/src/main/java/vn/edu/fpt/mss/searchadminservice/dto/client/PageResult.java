package vn.edu.fpt.mss.searchadminservice.dto.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Trang dữ liệu content-service trả về. content-service gửi cả dạng phẳng (number, size, ...) lẫn đối tượng lồng
 * "page": {...} cho FE cũ, nên ở đây dùng "number" cho số trang và bỏ qua mọi field lạ (trong đó có "page").
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PageResult<T>(List<T> content, int number, int size, long totalElements, int totalPages, boolean last) {
    public static <T> PageResult<T> empty() {
        return new PageResult<>(List.of(), 0, 0, 0, 0, true);
    }
}
