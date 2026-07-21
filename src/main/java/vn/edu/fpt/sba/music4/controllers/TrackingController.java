package vn.edu.fpt.sba.music4.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.request.SyncTimeRequest;
import vn.edu.fpt.sba.music4.dtos.request.TrackPlayRequest;
import vn.edu.fpt.sba.music4.dtos.response.TrackResponseDTO;
import vn.edu.fpt.sba.music4.entities.User;
import vn.edu.fpt.sba.music4.repositories.UserRepository;
import vn.edu.fpt.sba.music4.services.ITrackingService;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tracking & History API", description = "Các API xử lý ghi nhận lịch sử nghe nhạc và đồng bộ thời gian phát của người dùng")
public class TrackingController {

    private final ITrackingService trackingService;
    private final UserRepository userRepository;

    @PostMapping("/play")
    @Operation(summary = "Ghi nhận lượt nghe nhạc", description = "Lưu lại lịch sử nghe nhạc của user vào DB và tự động tăng view_count cho bài hát. Hỗ trợ xử lý bất đồng bộ (@Async) để không làm chậm trải nghiệm.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ghi nhận lượt nghe thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ (VD: thiếu trackId)")
    })
    public ResponseEntity<String> recordPlayHistory(@Valid @RequestBody TrackPlayRequest request, Principal principal) {
        trackingService.recordPlayHistory(request.trackId(), request.userId(), principal.getName());

        return ResponseEntity.ok("Listen recorded successfully!");
    }

    @PutMapping("/sync-time")
    @Operation(summary = "Đồng bộ thời gian nghe dở", description = "Cập nhật vị trí thời gian (tính bằng giây) mà user đang phát bài hát, giúp tính năng 'Nghe tiếp tục' hoạt động.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đồng bộ thời gian thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ (VD: số giây âm)")
    })
    public ResponseEntity<String> syncPlaybackTime(@Valid @RequestBody SyncTimeRequest request) {
        trackingService.syncPlaybackPosition(request);
        return ResponseEntity.ok("Đồng bộ thời gian thành công!");
    }

    @GetMapping("/history/{userId}")
    @Operation(summary = "Lấy lịch sử nghe nhạc của người dùng", description = "Trả về danh sách các bài hát mà người dùng đã nghe gần đây, được sắp xếp theo thời gian nghe mới nhất lên đầu.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách lịch sử thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thông tin người dùng")
    })
    public ResponseEntity<Page<TrackResponseDTO>> getHistory(@PathVariable Integer userId,
                                                             @PageableDefault(size = 20, page = 0) Pageable pageable) {
        return ResponseEntity.ok(trackingService.getListeningHistory(userId, pageable));
    }

    @PostMapping("/history")
    public ResponseEntity<?> saveHistory(@RequestBody @Valid TrackPlayRequest request) {
        try {
            trackingService.saveHistory(request);
            return ResponseEntity.ok("Lưu lịch sử nghe nhạc thành công!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi: " + e.getMessage());
        }
    }

    @PostMapping("/cleanup-history")
    public ResponseEntity<String> cleanupHistory() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập để sử dụng tính năng này!");
        }

        User user = userRepository.findByUserName(auth.getName()).orElse(null);
        if (user == null || !"ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Chỉ ADMIN mới được phép dọn dẹp lịch sử!");
        }

        log.info("[ADMIN] {} kích hoạt dọn dẹp lịch sử thủ công...", user.getUsername());
        trackingService.cleanupHistory();
        return ResponseEntity.ok("Đã dọn dẹp lịch sử thành công!");
    }
}