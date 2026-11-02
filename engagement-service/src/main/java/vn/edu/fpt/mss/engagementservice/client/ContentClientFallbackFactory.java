package vn.edu.fpt.mss.engagementservice.client;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import vn.edu.fpt.mss.engagementservice.dto.client.ArtistSummary;
import vn.edu.fpt.mss.engagementservice.dto.client.TrackSummary;
import vn.edu.fpt.mss.engagementservice.exception.ResourceNotFoundException;
import vn.edu.fpt.mss.engagementservice.exception.ServiceUnavailableException;

import java.util.List;

/**
 * Circuit breaker fallback: đọc danh sách thì trả rỗng (FE vẫn hiển thị playlist, bài nào thiếu thì đánh dấu
 * available=false); kiểm tra tồn tại thì báo 404/503 rõ ràng.
 */
@Slf4j
@Component
public class ContentClientFallbackFactory implements FallbackFactory<ContentClient> {

    @Override
    public ContentClient create(Throwable cause) {
        return new ContentClient() {
            @Override
            public List<TrackSummary> getTracks(List<Integer> ids) {
                log.warn("content-service lỗi, trả danh sách bài rỗng: {}", cause.getMessage());
                return List.of();
            }

            @Override
            public List<ArtistSummary> getArtists(List<Integer> ids) {
                log.warn("content-service lỗi, trả danh sách nghệ sĩ rỗng: {}", cause.getMessage());
                return List.of();
            }

            @Override
            public ArtistSummary getArtist(Integer id) {
                if (cause instanceof FeignException.NotFound) {
                    throw new ResourceNotFoundException("Không tìm thấy nghệ sĩ id=" + id);
                }
                throw new ServiceUnavailableException("Không kiểm tra được nghệ sĩ, vui lòng thử lại sau");
            }
        };
    }
}
