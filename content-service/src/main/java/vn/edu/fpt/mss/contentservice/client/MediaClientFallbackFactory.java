package vn.edu.fpt.mss.contentservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import vn.edu.fpt.mss.contentservice.exception.BadRequestException;
import vn.edu.fpt.mss.contentservice.exception.ServiceUnavailableException;

/** Upload không có fallback hợp lý: trả lỗi rõ ràng thay vì treo request. Lỗi 4xx của media giữ nguyên ý nghĩa. */
@Slf4j
@Component
public class MediaClientFallbackFactory implements FallbackFactory<MediaClient> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public MediaClient create(Throwable cause) {
        return (file, type, uploadedBy) -> {
            if (cause instanceof feign.FeignException fe && fe.status() >= 400 && fe.status() < 500) {
                String errorMsg = extractErrorMessage(fe.contentUTF8());
                throw new BadRequestException(errorMsg);
            }
            log.error("media-service lỗi khi upload: {}", cause.getMessage());
            throw new ServiceUnavailableException("Dịch vụ lưu trữ file đang bận, vui lòng thử lại sau");
        };
    }

    private String extractErrorMessage(String body) {
        if (body != null && !body.isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(body);
                if (node.hasNonNull("message")) {
                    return node.get("message").asText();
                }
            } catch (Exception ignored) {
            }
        }
        return "File không hợp lệ hoặc không đúng định dạng cho phép";
    }
}
