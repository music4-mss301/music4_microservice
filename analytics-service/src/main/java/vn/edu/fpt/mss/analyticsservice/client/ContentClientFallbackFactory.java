package vn.edu.fpt.mss.analyticsservice.client;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import vn.edu.fpt.mss.analyticsservice.dto.client.ArtistStats;
import vn.edu.fpt.mss.analyticsservice.dto.client.TrackSummary;
import vn.edu.fpt.mss.analyticsservice.exception.ResourceNotFoundException;
import vn.edu.fpt.mss.analyticsservice.exception.ServiceUnavailableException;

import java.util.List;

@Slf4j
@Component
public class ContentClientFallbackFactory implements FallbackFactory<ContentClient> {
    @Override
    public ContentClient create(Throwable cause) {
        return new ContentClient() {
            @Override
            public List<TrackSummary> getTracks(List<Integer> ids) {
                log.warn("content-service lỗi (getTracks): {}", cause.getMessage());
                return List.of();
            }

            @Override
            public List<TrackSummary> trending(int limit) {
                log.warn("content-service lỗi (trending): {}", cause.getMessage());
                return List.of();
            }

            @Override
            public ArtistStats artistStats(Integer artistId) {
                if (cause instanceof FeignException.NotFound) {
                    throw new ResourceNotFoundException("Không tìm thấy nghệ sĩ id=" + artistId);
                }
                throw new ServiceUnavailableException("Không lấy được dữ liệu nghệ sĩ, vui lòng thử lại sau");
            }
        };
    }
}
