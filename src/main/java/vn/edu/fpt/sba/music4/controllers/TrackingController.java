package vn.edu.fpt.sba.music4.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.request.SyncTimeRequest;
import vn.edu.fpt.sba.music4.dtos.request.TrackPlayRequest;
import vn.edu.fpt.sba.music4.dtos.response.TrackResponseDTO;
import vn.edu.fpt.sba.music4.services.ITrackingService;

import java.util.List;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
@Tag(name = "Tracking & History API", description = "Các API xử lý ghi nhận lịch sử nghe nhạc và đồng bộ thời gian phát của người dùng")
public class TrackingController {

    private final ITrackingService trackingService;

    @PostMapping("/play")
    @Operation(summary = "Ghi nhận lượt nghe nhạc", description = "Lưu lại lịch sử nghe nhạc của user vào DB và tự động tăng view_count cho bài hát. Hỗ trợ xử lý bất đồng bộ (@Async) để không làm chậm trải nghiệm.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ghi nhận lượt nghe thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ (VD: thiếu trackId)")
    })
    public ResponseEntity<String> recordPlayHistory(@Valid @RequestBody TrackPlayRequest request) {
        trackingService.recordPlayHistory(request.trackId(), request.userId());

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
        return ResponseEntity.ok("Time synchronized successfully!");
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
}