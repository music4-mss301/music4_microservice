package vn.edu.fpt.mss.contentservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.mss.contentservice.config.RabbitTopologyConfig;
import vn.edu.fpt.mss.contentservice.dto.event.TrackPlayedPayload;
import vn.edu.fpt.mss.contentservice.entity.ProcessedEvent;
import vn.edu.fpt.mss.contentservice.repository.ProcessedEventRepository;
import vn.edu.fpt.mss.contentservice.service.TrackService;

import java.time.LocalDateTime;

/** content-service là chủ view_count: analytics chỉ bắn event, content tự cộng (idempotent theo eventId). */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrackPlayedListener {

    private final TrackService trackService;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    @RabbitListener(queues = RabbitTopologyConfig.Q_TRACK_PLAYED)
    public void onTrackPlayed(EventEnvelope event) {
        if (processedEventRepository.existsById(event.eventId())) {
            log.debug("Bỏ qua event đã xử lý {}", event.eventId());
            return;
        }
        TrackPlayedPayload payload = objectMapper.convertValue(event.payload(), TrackPlayedPayload.class);
        trackService.incrementView(payload.trackId());
        processedEventRepository.save(new ProcessedEvent(event.eventId(), LocalDateTime.now()));
    }
}
