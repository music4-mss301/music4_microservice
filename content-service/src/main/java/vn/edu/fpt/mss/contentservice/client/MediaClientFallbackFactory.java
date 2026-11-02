package vn.edu.fpt.mss.contentservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import vn.edu.fpt.mss.contentservice.exception.BadRequestException;
import vn.edu.fpt.mss.contentservice.exception.ServiceUnavailableException;

/** Upload không có fallback hợp lý: trả lỗi rõ ràng thay vì treo request. Lỗi 4xx của media giữ nguyên ý nghĩa. */
@Slf4j
@Component
public class MediaClientFallbackFactory implements FallbackFactory<MediaClient> {
    @Override
    public MediaClient create(Throwable cause) {
        return (file, type, uploadedBy) -> {
            if (cause instanceof feign.FeignException fe && fe.status() >= 400 && fe.status() < 500) {
                throw new BadRequestException("File không hợp lệ: " + fe.contentUTF8());
            }
            log.error("media-service lỗi khi upload: {}", cause.getMessage());
            throw new ServiceUnavailableException("Dịch vụ lưu trữ file đang bận, vui lòng thử lại sau");
        };
    }
}
