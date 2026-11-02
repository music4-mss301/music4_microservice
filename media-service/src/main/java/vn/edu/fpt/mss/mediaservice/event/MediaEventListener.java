package vn.edu.fpt.mss.mediaservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import vn.edu.fpt.mss.mediaservice.config.RabbitTopologyConfig;
import vn.edu.fpt.mss.mediaservice.service.MediaService;

import java.util.ArrayList;
import java.util.List;

/** content.track.deleted: xóa file audio trên S3. Ảnh bìa không xóa vì có thể dùng chung với album. */
@Slf4j
@Component
@RequiredArgsConstructor
public class MediaEventListener {

    private final MediaService mediaService;

    @RabbitListener(queues = RabbitTopologyConfig.Q_EVENTS)
    public void onEvent(EventEnvelope event) {
        if (!EventTypes.TRACK_DELETED.equals(event.type())) {
            return;
        }
        List<String> keys = new ArrayList<>();
        for (String field : List.of("filePath", "previewPath")) {
            if (event.payload().get(field) instanceof String key && !key.isBlank() && !keys.contains(key)) {
                keys.add(key);
            }
        }
        if (!keys.isEmpty()) {
            log.info("Đã xóa {} file của track {}", mediaService.deleteByKeys(keys), event.payload().get("trackId"));
        }
    }
}
