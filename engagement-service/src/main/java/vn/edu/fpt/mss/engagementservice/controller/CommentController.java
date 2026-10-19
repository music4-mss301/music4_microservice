package vn.edu.fpt.mss.engagementservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.mss.engagementservice.dto.request.CommentRequest;
import vn.edu.fpt.mss.engagementservice.dto.request.CommentUpdateRequest;
import vn.edu.fpt.mss.engagementservice.dto.response.CommentResponse;
import vn.edu.fpt.mss.engagementservice.dto.response.PageResponse;
import vn.edu.fpt.mss.engagementservice.service.CommentService;

@Tag(name = "Comments")
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/track/{trackId}")
    public PageResponse<CommentResponse> byTrack(@PathVariable Integer trackId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return commentService.getByTrack(trackId, page, size);
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(@Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.create(request));
    }

    @PutMapping("/{id}")
    public CommentResponse update(@PathVariable Integer id, @Valid @RequestBody CommentUpdateRequest request) {
        return commentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        commentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
