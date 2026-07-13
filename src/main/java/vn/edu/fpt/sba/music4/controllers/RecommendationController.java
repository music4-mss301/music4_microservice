package vn.edu.fpt.sba.music4.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.response.TrackSuggestResponse;
import vn.edu.fpt.sba.music4.entities.User;
import vn.edu.fpt.sba.music4.repositories.UserRepository;
import vn.edu.fpt.sba.music4.services.IRecommendationService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(name = "AI Recommendation API", description = "API Gợi ý bài hát thông minh dựa trên lịch sử nghe và sở thích của người dùng (Content-Based Filtering)")
public class RecommendationController {

    private final IRecommendationService recommendationService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Lấy danh sách bài hát gợi ý", description = "Trả về Top 10 bài hát phù hợp nhất với Gu âm nhạc của User (có kèm điểm matchScore). Nếu User chưa đăng nhập (userId = null) hoặc chưa có dữ liệu, tự động trả về danh sách Trending (nhạc Hot).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách gợi ý thành công"),
            @ApiResponse(responseCode = "400", description = "Lỗi kiểu dữ liệu tham số userId truyền vào")
    })
    public ResponseEntity<?> getRecommendations() {
        // 1. Lấy thông tin user hiện tại từ Security Context (đã được JwtFilter xác thực)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Nếu không có token hoặc đang là Guest vô danh -> ko cho
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập để sử dụng tính năng này!");
        }

        // 2. Lấy username từ token
        String username = auth.getName();

        // 3. Truy vấn Database để lấy userId
        User user = userRepository.findByUserName(username)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Tài khoản không tồn tại!");
        }

        // 4. Truyền đúng userId cho AI xử lý
        List<TrackSuggestResponse> suggestions = recommendationService.getRecommendations(user.getId());
        return ResponseEntity.ok(suggestions);
    }

    @PostMapping("/sync")
    @Operation(summary = "Đồng bộ nhạc lên ES (Chạy ngầm)", description = "Lấy toàn bộ nhạc trong SQL -> Dịch sang Vector -> Đẩy vào Elasticsearch. API này không block (không treo), trả về phản hồi ngay lập tức.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Đã tiếp nhận yêu cầu, tiến trình đồng bộ Vector đang được chạy ngầm (Accepted)"),
            @ApiResponse(responseCode = "500", description = "Lỗi hệ thống trong quá trình khởi tạo đồng bộ")
    })
    public ResponseEntity<String> syncDataToElasticsearch() {
        recommendationService.syncTracksToElasticsearch();
        return ResponseEntity.accepted().body("Tiến trình đồng bộ Vector đang được chạy ngầm. Vui lòng kiểm tra màn hình console (log) của hệ thống để xem tiến độ!");
    }
}