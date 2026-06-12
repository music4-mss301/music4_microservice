package vn.edu.fpt.sba.music4.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.response.FavoriteResponseDTO;
import vn.edu.fpt.sba.music4.services.IFavoriteService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Favorite API", description = "Quản lý danh sách bài hát yêu thích của người dùng")
public class FavoriteController {

    private final IFavoriteService favoriteService;

    @PostMapping("/toggle/{trackId}")
    @Operation(
            summary = "Thêm/Xóa bài hát khỏi danh sách yêu thích (Like/Unlike)",
            description = "Nếu bài hát chưa được thích thì sẽ Thêm (liked: true). Nếu bài hát đã được thích rồi thì sẽ Xóa (liked: false)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Xử lý trạng thái yêu thích thành công",
            content = @Content(
                    mediaType = "application/json",
                    examples = {
                            @ExampleObject(
                                    name = "Trường hợp Thêm (Like)",
                                    value = "{\"liked\": true, \"message\": \"Đã thêm bài hát vào danh sách yêu thích!\"}"
                            ),
                            @ExampleObject(
                                    name = "Trường hợp Xóa (Unlike)",
                                    value = "{\"liked\": false, \"message\": \"Đã xóa bài hát khỏi danh sách yêu thích!\"}"
                            )
                    }
            )
    )
    public ResponseEntity<?> toggleFavorite(
            @Parameter(description = "ID của Bài hát cần Like/Unlike", example = "10") @PathVariable Integer trackId) {
        boolean isLiked = favoriteService.toggleFavorite(trackId);

        return ResponseEntity.ok(Map.of(
                "liked", isLiked,
                "message", isLiked ? "Đã thêm bài hát vào danh sách yêu thích!" : "Đã xóa bài hát khỏi danh sách yêu thích!"
        ));
    }

    @GetMapping("/me")
    @Operation(summary = "Lấy danh sách bài hát yêu thích của tôi", description = "Hiển thị toàn bộ các bài hát mà người dùng hiện tại đã nhấn thích")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    public ResponseEntity<List<FavoriteResponseDTO>> getMyFavorites() {
        return ResponseEntity.ok(favoriteService.getMyFavorites());
    }
}