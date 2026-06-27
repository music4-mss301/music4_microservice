package vn.edu.fpt.sba.music4.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.request.CommentRequest;
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

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Integer commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Xóa bình luận thành công!"
        ));
    }
}