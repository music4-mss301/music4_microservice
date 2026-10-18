package vn.edu.fpt.mss.analyticsservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/** search-admin/ES chết: trả rỗng, RecommendationService tự chuyển sang trending. */
@Slf4j
@Component
public class SearchClientFallbackFactory implements FallbackFactory<SearchClient> {
    @Override
    public SearchClient create(Throwable cause) {
        return query -> {
            log.warn("search-admin-service lỗi, dùng trending: {}", cause.getMessage());
            return List.of();
        };
    }
}
