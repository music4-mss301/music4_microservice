package vn.edu.fpt.mss.engagementservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Đẩy thông báo thời gian thực (SSE). Một người dùng mở nhiều tab thì mỗi tab một kết nối. */
@Slf4j
@Component
public class NotificationStreamService {

    private static final long TIMEOUT_MS = 24L * 60 * 60 * 1000;
    private final Map<Integer, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Integer userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));
        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected successfully!"));
        } catch (IOException e) {
            remove(userId, emitter);
        }
        return emitter;
    }

    /** eventName khớp FE đang lắng nghe: NEW_TRACK, NEW_ALBUM. */
    public void push(Integer userId, String eventName, Object data) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException | IllegalStateException e) {
                remove(userId, emitter);
            }
        }
    }

    private void remove(Integer userId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }
}
