package vn.edu.fpt.mss.engagementservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.edu.fpt.mss.engagementservice.dto.response.NotificationResponse;
import vn.edu.fpt.mss.engagementservice.dto.response.PageResponse;
import vn.edu.fpt.mss.engagementservice.security.SecurityUtils;
import vn.edu.fpt.mss.engagementservice.service.NotificationService;
import vn.edu.fpt.mss.engagementservice.service.NotificationStreamService;

import java.util.List;
import java.util.Map;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationStreamService streamService;

    /** Dạng mảng, giữ như monolith (FE: res.data.filter). */
    @GetMapping("/user/{userId}")
    public List<NotificationResponse> ofUser(@PathVariable Integer userId) {
        return notificationService.listOf(userId);
    }

    @GetMapping("/me")
    public PageResponse<NotificationResponse> mine(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return notificationService.getOf(SecurityUtils.requireUser().id(), page, size);
    }

    /**
     * Thông báo thời gian thực (SSE). EventSource không gửi được header Authorization nên FE truyền token qua
     * ?token=... (gateway đã cho phép riêng cho đường dẫn này). Chỉ được nghe thông báo của chính mình.
     */
    @GetMapping(value = "/subscribe/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable Integer userId) {
        SecurityUtils.requireOwnerOrAdmin(userId);
        return streamService.subscribe(userId);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread() {
        return Map.of("unread", notificationService.unreadCount());
    }

    @PutMapping("/{id}/read")
    public Map<String, Object> markRead(@PathVariable Integer id) {
        notificationService.markRead(id);
        return Map.of("success", true, "message", "Đã đọc thông báo!");
    }

    @PutMapping("/read-all")
    public Map<String, Integer> markAllRead() {
        return Map.of("updated", notificationService.markAllRead());
    }

    @DeleteMapping("/clear-all")
    public ResponseEntity<String> clearAll() {
        notificationService.clearAll();
        return ResponseEntity.ok("Đã xóa sạch thông báo!");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Integer id) {
        notificationService.delete(id);
        return ResponseEntity.ok("Đã xóa thông báo thành công!");
    }
}
