package vn.edu.fpt.sba.music4.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.request.CommentRequest;
import vn.edu.fpt.sba.music4.dtos.request.UpdateCommentRequest; // <-- Thêm import này
import vn.edu.fpt.sba.music4.dtos.response.CommentResponseDTO;
import vn.edu.fpt.sba.music4.services.ICommentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CommentController {

    private final ICommentService commentService;

    @PostMapping
    public ResponseEntity<?> addComment(@Valid @RequestBody CommentRequest request) {
        commentService.addComment(request);
        return ResponseEntity.ok(Map.of("message", "Đăng bình luận thành công!"));
    }

    @GetMapping("/track/{trackId}")
    public ResponseEntity<List<CommentResponseDTO>> getCommentsByTrack(@PathVariable Integer trackId) {
        return ResponseEntity.ok(commentService.getCommentsByTrack(trackId));
    }

    // Bổ sung method PUT để nhận yêu cầu chỉnh sửa bình luận từ Frontend
    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(
            @PathVariable Integer commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        commentService.updateComment(commentId, request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cập nhật bình luận thành công!"
        ));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Integer commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Xóa bình luận thành công!"
        ));
    }
}