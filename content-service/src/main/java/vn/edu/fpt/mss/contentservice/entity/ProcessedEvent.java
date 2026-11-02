package vn.edu.fpt.mss.contentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Lưu eventId đã xử lý để consumer idempotent (RabbitMQ có thể giao lại cùng một message). */
@Entity
@Table(name = "processed_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {
    @Id
    @Column(name = "event_id", length = 64)
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
}
