package vn.edu.fpt.mss.contentservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/** engagement chết: top nghệ sĩ vẫn trả về, followers và favorites hiển thị 0. */
@Slf4j
@Component
public class EngagementClientFallbackFactory implements FallbackFactory<EngagementClient> {
    @Override
    public EngagementClient create(Throwable cause) {
        return queries -> {
            log.warn("engagement-service lỗi, followers/favorites = 0: {}", cause.getMessage());
            return List.of();
        };
    }
}
