package vn.edu.fpt.mss.searchadminservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import vn.edu.fpt.mss.searchadminservice.config.RabbitTopologyConfig;
import vn.edu.fpt.mss.searchadminservice.dto.client.AlbumDoc;
import vn.edu.fpt.mss.searchadminservice.dto.client.ArtistDoc;
import vn.edu.fpt.mss.searchadminservice.dto.client.TrackDoc;
import vn.edu.fpt.mss.searchadminservice.service.IndexingService;

import java.util.Map;

/**
 * Đồng bộ ES theo event của content-service. Upsert/delete theo id nên tự nhiên idempotent.
 * Lỗi (ES chết) => exception => RabbitMQ retry 3 lần rồi vào DLQ; admin có thể reindex để cứu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentSyncListener {

    private final IndexingService indexingService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitTopologyConfig.Q_EVENTS)
    public void onEvent(EventEnvelope event) {
        Map<String, Object> p = event.payload();
        switch (event.type()) {
            case EventTypes.TRACK_CREATED, EventTypes.TRACK_UPDATED -> indexingService.indexTrack(as(p, TrackDoc.class));
            case EventTypes.TRACK_DELETED -> indexingService.deleteTrack(intOf(p, "trackId"));
            case EventTypes.ALBUM_CREATED, EventTypes.ALBUM_UPDATED -> indexingService.indexAlbum(as(p, AlbumDoc.class));
            case EventTypes.ALBUM_DELETED -> indexingService.deleteAlbum(intOf(p, "albumId"));
            case EventTypes.ARTIST_CREATED, EventTypes.ARTIST_UPDATED -> indexingService.indexArtist(as(p, ArtistDoc.class));
            case EventTypes.ARTIST_DELETED -> indexingService.deleteArtist(intOf(p, "artistId"));
            case EventTypes.CATEGORY_UPDATED -> indexingService.renameCategory(intOf(p, "id"), String.valueOf(p.get("name")));
            case EventTypes.CATEGORY_DELETED -> indexingService.removeCategory(intOf(p, "id"));
            default -> log.debug("Bỏ qua event {}", event.type());
        }
    }

    private <T> T as(Map<String, Object> payload, Class<T> type) {
        return objectMapper.convertValue(payload, type);
    }

    private Integer intOf(Map<String, Object> payload, String field) {
        return payload.get(field) instanceof Number n ? n.intValue() : null;
    }
}
