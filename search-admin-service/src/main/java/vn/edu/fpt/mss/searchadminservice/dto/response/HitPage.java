package vn.edu.fpt.mss.searchadminservice.dto.response;

import java.util.List;

public record HitPage<T>(List<T> content, int page, int size, long totalElements) {
    public static <T> HitPage<T> empty(int page, int size) {
        return new HitPage<>(List.of(), page, size, 0);
    }
}
