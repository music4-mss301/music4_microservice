package vn.edu.fpt.mss.searchadminservice.event;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/** Envelope chung cho mọi event trên exchange music4.events (xem Music4-refactor-events.md). */
public record EventEnvelope(String eventId, String type, int version, OffsetDateTime occurredAt,
                            String producer, Map<String, Object> payload) {

    public static EventEnvelope of(String type, String producer, Map<String, Object> payload) {
        return new EventEnvelope(UUID.randomUUID().toString(), type, 1, OffsetDateTime.now(), producer, payload);
    }
}
