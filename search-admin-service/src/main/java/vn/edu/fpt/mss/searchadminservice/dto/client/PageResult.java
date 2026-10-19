package vn.edu.fpt.mss.searchadminservice.dto.client;

import java.util.List;

public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages, boolean last) {
    public static <T> PageResult<T> empty() {
        return new PageResult<>(List.of(), 0, 0, 0, 0, true);
    }
}
