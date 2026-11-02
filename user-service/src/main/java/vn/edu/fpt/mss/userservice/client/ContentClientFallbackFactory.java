package vn.edu.fpt.mss.userservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import vn.edu.fpt.mss.userservice.exception.ServiceUnavailableException;

/** Không có fallback hợp lý cho việc tạo hồ sơ nghệ sĩ: báo 503 để transaction đăng ký rollback. */
@Slf4j
@Component
public class ContentClientFallbackFactory implements FallbackFactory<ContentClient> {
    @Override
    public ContentClient create(Throwable cause) {
        return request -> {
            log.error("content-service không phản hồi khi tạo artist profile: {}", cause.getMessage());
            throw new ServiceUnavailableException("Không tạo được hồ sơ nghệ sĩ, vui lòng thử lại sau");
        };
    }
}
