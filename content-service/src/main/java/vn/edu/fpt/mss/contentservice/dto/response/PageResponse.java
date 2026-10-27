package vn.edu.fpt.mss.contentservice.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Trả cả hai kiểu để FE cũ và mới đều đọc được:
 *  - dạng phẳng: content, number, size, totalElements, totalPages, last
 *  - dạng lồng của Spring Data (FE đang đọc res.data.page.totalPages): page { size, number, totalElements, totalPages }
 */
public record PageResponse<T>(List<T> content, PageMeta page, int number, int size, long totalElements,
                              int totalPages, boolean last) {

    public record PageMeta(int size, int number, long totalElements, int totalPages) {
    }

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                new PageMeta(page.getSize(), page.getNumber(), page.getTotalElements(), page.getTotalPages()),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return of(page, Function.identity());
    }

    /** Tự dựng trang từ danh sách đã cắt sẵn (dùng cho random theo seed, lịch sử nghe...). */
    public static <T> PageResponse<T> of(List<T> content, int pageNumber, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil(totalElements / (double) size);
        return new PageResponse<>(content, new PageMeta(size, pageNumber, totalElements, totalPages), pageNumber,
                size, totalElements, totalPages, pageNumber >= totalPages - 1);
    }
}
