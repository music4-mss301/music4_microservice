package vn.edu.fpt.sba.music4.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.edu.fpt.sba.music4.dtos.response.NotificationResponseDTO;
import vn.edu.fpt.sba.music4.services.INotificationService;
import vn.edu.fpt.sba.music4.services.impl.NotificationServiceImpl;

import java.nio.file.attribute.UserPrincipal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final INotificationService notificationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponseDTO>> getMyNotifications(@PathVariable Integer userId) {
        return ResponseEntity.ok(notificationService.getMyNotifications(userId));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Integer id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Đã đọc thông báo!"));
    }
    @GetMapping(value = "/subscribe/{userId}", produces = "text/event-stream")
    public SseEmitter subscribeNotifications(@PathVariable Integer userId) {
        return ((NotificationServiceImpl) notificationService).subscribe(userId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Integer id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok("Đã xóa thông báo thành công!");
    }

    @DeleteMapping("/clear-all")
    public ResponseEntity<?> deleteAllMyNotifications(
            @AuthenticationPrincipal vn.edu.fpt.sba.music4.entities.User currentUser
    ) {
        notificationService.deleteAllMyNotifications(currentUser.getId());
        return ResponseEntity.ok("Đã xóa sạch thông báo!");
    }
}