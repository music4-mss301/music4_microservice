package vn.edu.fpt.mss.searchadminservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record HitPage<T>(List<T> content, int page, int size, long totalElements) {
    public static <T> HitPage<T> empty(int page, int size) {
        return new HitPage<>(List.of(), page, size, 0);
    }

    @JsonProperty("totalPages")
    public int totalPages() {
        return size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }
}
