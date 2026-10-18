package vn.edu.fpt.mss.analyticsservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import vn.edu.fpt.mss.analyticsservice.config.RabbitTopologyConfig;
import vn.edu.fpt.mss.analyticsservice.service.TrackingService;

/** user.account.deleted: ẩn danh lịch sử (giữ số liệu thống kê, bỏ liên kết với người dùng). Idempotent tự nhiên. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsEventListener {

    private final TrackingService trackingService;

    @RabbitListener(queues = RabbitTopologyConfig.Q_EVENTS)
    public void onEvent(EventEnvelope event) {
        if (EventTypes.USER_DELETED.equals(event.type()) && event.payload().get("userId") instanceof Number id) {
            trackingService.anonymizeUser(id.intValue());
            log.info("Đã ẩn danh lịch sử nghe của user {}", id);
        }
    }
}
