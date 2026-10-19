package vn.edu.fpt.mss.engagementservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.mss.engagementservice.config.RabbitTopologyConfig;
import vn.edu.fpt.mss.engagementservice.dto.event.IdPayload;
import vn.edu.fpt.mss.engagementservice.dto.event.TrackEventPayload;
import vn.edu.fpt.mss.engagementservice.entity.ProcessedEvent;
import vn.edu.fpt.mss.engagementservice.repository.ProcessedEventRepository;
import vn.edu.fpt.mss.engagementservice.service.InsightService;
import vn.edu.fpt.mss.engagementservice.service.NotificationService;

import java.time.LocalDateTime;

/** Một queue, nhiều routing key: phân nhánh theo envelope.type. Idempotent theo eventId. */
@Slf4j
@Component
@RequiredArgsConstructor
public class EngagementEventListener {

    private final NotificationService notificationService;
    private final InsightService insightService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    @RabbitListener(queues = RabbitTopologyConfig.Q_EVENTS)
    public void onEvent(EventEnvelope event) {
        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }
        switch (event.type()) {
            case EventTypes.TRACK_CREATED -> {
                int n = notificationService.notifyFollowersOfNewTrack(convert(event, TrackEventPayload.class));
                log.info("Tạo {} thông báo cho bài mới", n);
            }
            case EventTypes.TRACK_DELETED -> insightService.cleanupTrack(convert(event, TrackEventPayload.class).trackId());
            case EventTypes.ARTIST_DELETED -> insightService.cleanupArtist(convert(event, IdPayload.class).artistId());
            case EventTypes.USER_DELETED -> insightService.cleanupUser(convert(event, IdPayload.class).userId());
            default -> log.debug("Bỏ qua event {}", event.type());
        }
        processedEventRepository.save(new ProcessedEvent(event.eventId(), LocalDateTime.now()));
    }

    private <T> T convert(EventEnvelope event, Class<T> type) {
        return objectMapper.convertValue(event.payload(), type);
    }
}
