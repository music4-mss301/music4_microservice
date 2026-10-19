package vn.edu.fpt.mss.searchadminservice.client;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import vn.edu.fpt.mss.searchadminservice.dto.client.PageResult;
import vn.edu.fpt.mss.searchadminservice.dto.client.StatsDtos;
import vn.edu.fpt.mss.searchadminservice.dto.client.TrackDoc;
import vn.edu.fpt.mss.searchadminservice.dto.client.TrackSummary;
import vn.edu.fpt.mss.searchadminservice.exception.BadRequestException;
import vn.edu.fpt.mss.searchadminservice.exception.ServiceUnavailableException;

/**
 * Fallback cho dashboard: service nào chết thì ô số liệu đó trả null (FE hiển thị "không khả dụng"),
 * dashboard vẫn chạy. Thao tác ghi (cleanup, reindex) thì báo 503 rõ ràng.
 */
@Slf4j
public final class AdminClientsFallbacks {

    private AdminClientsFallbacks() {
    }

    @Component
    public static class Content implements FallbackFactory<ContentClient> {
        @Override
        public ContentClient create(Throwable cause) {
            return new ContentClient() {
                @Override
                public PageResult<TrackDoc> export(int page, int size) {
                    throw new ServiceUnavailableException("content-service không phản hồi, chưa reindex được");
                }

                @Override
                public PageResult<TrackSummary> search(String keyword, int page, int size) {
                    log.warn("content-service lỗi khi search fallback: {}", cause.getMessage());
                    return PageResult.empty();
                }

                @Override
                public StatsDtos.ContentStats stats() {
                    return null;
                }
            };
        }
    }

    @Component
    public static class User implements FallbackFactory<UserClient> {
        @Override
        public UserClient create(Throwable cause) {
            return () -> null;
        }
    }

    @Component
    public static class Engagement implements FallbackFactory<EngagementClient> {
        @Override
        public EngagementClient create(Throwable cause) {
            return () -> null;
        }
    }

    @Component
    public static class Analytics implements FallbackFactory<AnalyticsClient> {
        @Override
        public AnalyticsClient create(Throwable cause) {
            return new AnalyticsClient() {
                @Override
                public StatsDtos.AnalyticsStats stats() {
                    return null;
                }

                @Override
                public java.util.Map<String, Integer> cleanup(int days) {
                    if (cause instanceof FeignException fe && fe.status() == 400) {
                        throw new BadRequestException("Số ngày không hợp lệ (tối thiểu 30)");
                    }
                    throw new ServiceUnavailableException("analytics-service không phản hồi");
                }
            };
        }
    }
}
