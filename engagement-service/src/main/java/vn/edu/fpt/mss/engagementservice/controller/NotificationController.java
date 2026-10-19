package vn.edu.fpt.mss.engagementservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.mss.engagementservice.dto.response.NotificationResponse;
import vn.edu.fpt.mss.engagementservice.dto.response.PageResponse;
import vn.edu.fpt.mss.engagementservice.security.SecurityUtils;
import vn.edu.fpt.mss.engagementservice.service.NotificationService;

import java.util.Map;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/me")
    public PageResponse<NotificationResponse> mine(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return notificationService.getOf(SecurityUtils.requireUser().id(), page, size);
    }

    @GetMapping("/user/{userId}")
    public PageResponse<NotificationResponse> ofUser(@PathVariable Integer userId,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        return notificationService.getOf(userId, page, size);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread() {
        return Map.of("unread", notificationService.unreadCount());
    }

    @PutMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Integer id) {
        return notificationService.markRead(id);
    }

    @PutMapping("/read-all")
    public Map<String, Integer> markAllRead() {
        return Map.of("updated", notificationService.markAllRead());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        notificationService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
