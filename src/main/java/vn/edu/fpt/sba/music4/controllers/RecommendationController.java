package vn.edu.fpt.sba.music4.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.response.TrackSuggestResponse;
import vn.edu.fpt.sba.music4.services.IRecommendationService;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@CrossOrigin("*")
@Tag(name = "AI Recommendation API", description = "API Gợi ý bài hát thông minh dựa trên lịch sử nghe và sở thích của người dùng (Content-Based Filtering)")
public class RecommendationController {

    private final IRecommendationService recommendationService;

    @GetMapping
    @Operation(summary = "Lấy danh sách bài hát gợi ý", description = "Trả về Top 10 bài hát phù hợp nhất với Gu âm nhạc của User (có kèm điểm matchScore). Nếu User chưa đăng nhập (userId = null) hoặc chưa có dữ liệu, tự động trả về danh sách Trending (nhạc Hot).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách gợi ý thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi kiểu dữ liệu tham số userId truyền vào")
    })
    public ResponseEntity<List<TrackSuggestResponse>> getRecommendations(
            @RequestParam(required = false) Integer userId) {

        List<TrackSuggestResponse> suggestions = recommendationService.getRecommendations(userId);
        return ResponseEntity.ok(suggestions);
    }
}