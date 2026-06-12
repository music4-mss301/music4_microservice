package vn.edu.fpt.sba.music4.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.request.RatingRequest;
import vn.edu.fpt.sba.music4.dtos.response.ArtistOverviewResponse;
import vn.edu.fpt.sba.music4.dtos.response.IChartData;
import vn.edu.fpt.sba.music4.services.IAnalyticsService;
import vn.edu.fpt.sba.music4.services.IRatingService;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics & Rating API", description = "Các API thống kê số liệu Dashboard cho Nghệ sĩ và thu thập đánh giá bài hát")
public class AnalyticsController {

    private final IAnalyticsService analyticsService;
    private final IRatingService ratingService;

    @GetMapping("/artist/{artistId}/views")
    @Operation(summary = "Lấy dữ liệu biểu đồ lượt nghe", description = "Trả về mảng dữ liệu (label/value) đại diện cho lượt nghe theo ngày của một nghệ sĩ để Frontend vẽ biểu đồ (Chart.js / Recharts).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy dữ liệu biểu đồ thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thông tin nghệ sĩ")
    })
    public ResponseEntity<List<IChartData>> getArtistDailyViews(@PathVariable Integer artistId) {
        return ResponseEntity.ok(analyticsService.getArtistDailyViews(artistId));
    }

    @PostMapping("/rating")
    @Operation(summary = "Đánh giá bài hát", description = "Thu thập điểm số đánh giá (1-5 sao) của người dùng. Dữ liệu này được dùng làm trọng số phạt/thưởng cho thuật toán AI Gợi ý.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lưu đánh giá thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ (VD: ratingValue nằm ngoài khoảng 1-5)")
    })
    public ResponseEntity<String> rateTrack(@Valid @RequestBody RatingRequest request) {
        ratingService.saveRating(request);
        return ResponseEntity.ok("Thank you for rating the song!");
    }

    @GetMapping("/artist/{artistId}/overview")
    @Operation(summary = "Lấy chỉ số tổng quan Dashboard", description = "Trả về 3 chỉ số quan trọng nhất của nghệ sĩ: Tổng lượt nghe, Tổng lượt thả tim, và Tổng số người theo dõi.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy dữ liệu tổng quan thành công"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy thông tin nghệ sĩ")
    })
    public ResponseEntity<ArtistOverviewResponse> getArtistOverview(@PathVariable Integer artistId) {
        return ResponseEntity.ok(analyticsService.getArtistOverview(artistId));
    }
}
