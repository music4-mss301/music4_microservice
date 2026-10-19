package vn.edu.fpt.mss.contentservice.event;

import vn.edu.fpt.mss.contentservice.config.RabbitConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

/**
 * Gửi event SAU KHI transaction commit, để không bắn event cho dữ liệu bị rollback.
 * Broker chết thì chỉ log, không làm hỏng nghiệp vụ chính (admin có thể reindex/đồng bộ lại).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.application.name}")
    private String producer;

    public void publish(String routingKey, Object payload) {
        Map<String, Object> body = objectMapper.convertValue(payload, new TypeReference<>() {
        });
        EventEnvelope envelope = EventEnvelope.of(routingKey, producer, body);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(envelope);
                }
            });
        } else {
            send(envelope);
        }
    }

    private void send(EventEnvelope envelope) {
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, envelope.type(), envelope);
            log.debug("Published {} ({})", envelope.type(), envelope.eventId());
        } catch (Exception ex) {
            log.error("Không gửi được event {}: {}", envelope.type(), ex.getMessage());
        }
    }
}
