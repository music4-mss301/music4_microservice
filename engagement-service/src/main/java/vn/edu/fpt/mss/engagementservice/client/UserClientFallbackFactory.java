package vn.edu.fpt.mss.engagementservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/** user-service chết: comment vẫn hiển thị, tên người dùng hiển thị mặc định. */
@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {
    @Override
    public UserClient create(Throwable cause) {
        return ids -> {
            log.warn("user-service lỗi, dùng tên mặc định: {}", cause.getMessage());
            return List.of();
        };
    }
}
