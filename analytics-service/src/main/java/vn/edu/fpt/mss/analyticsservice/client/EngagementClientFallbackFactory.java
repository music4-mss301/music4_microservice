package vn.edu.fpt.mss.analyticsservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import vn.edu.fpt.mss.analyticsservice.dto.client.ArtistEngagementStats;
import vn.edu.fpt.mss.analyticsservice.dto.client.UserSignals;

import java.util.List;

/** engagement chết: gợi ý vẫn chạy (rơi về trending), thống kê nghệ sĩ hiển thị 0. */
@Slf4j
@Component
public class EngagementClientFallbackFactory implements FallbackFactory<EngagementClient> {
    @Override
    public EngagementClient create(Throwable cause) {
        return new EngagementClient() {
            @Override
            public UserSignals signals(Integer userId) {
                log.warn("engagement-service lỗi, signals rỗng: {}", cause.getMessage());
                return UserSignals.empty(userId);
            }

            @Override
            public ArtistEngagementStats artistStats(Integer artistId, List<Integer> trackIds) {
                log.warn("engagement-service lỗi, stats = 0: {}", cause.getMessage());
                return new ArtistEngagementStats(artistId, 0, 0);
            }
        };
    }
}
