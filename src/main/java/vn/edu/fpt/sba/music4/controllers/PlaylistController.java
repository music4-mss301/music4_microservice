package vn.edu.fpt.sba.music4.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.sba.music4.dtos.request.AddTrackRequest;
import vn.edu.fpt.sba.music4.dtos.request.PlaylistRequest;
import vn.edu.fpt.sba.music4.dtos.response.PlaylistResponseDTO;
import vn.edu.fpt.sba.music4.dtos.response.TrackResponseDTO;
import vn.edu.fpt.sba.music4.services.IPlaylistService;

import java.util.List;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Playlist API", description = "Quản lý danh sách phát (Playlist) của người dùng")
public class PlaylistController {

    private final IPlaylistService playlistService;

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết thông tin Playlist", description = "Lấy thông tin cơ bản của một Playlist dựa theo ID")
    @ApiResponse(responseCode = "200", description = "Lấy chi tiết playlist thành công")
    public ResponseEntity<PlaylistResponseDTO> getPlaylistById(@PathVariable Integer id) {
        return ResponseEntity.ok(playlistService.getPlaylistById(id));
    }

    @GetMapping("/{id}/tracks")
    @Operation(summary = "Lấy danh sách bài hát của Playlist", description = "Lấy toàn bộ các bài hát nằm trong một Playlist cụ thể")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách bài hát thành công")
    public ResponseEntity<List<TrackResponseDTO>> getTracksByPlaylistId(@PathVariable Integer id) {
        return ResponseEntity.ok(playlistService.getTracksByPlaylistId(id));
    }

    @PostMapping
    @Operation(summary = "Tạo Playlist mới", description = "Tạo một playlist cá nhân mới cho người dùng")
    @ApiResponse(responseCode = "201", description = "Tạo playlist thành công")
    public ResponseEntity<PlaylistResponseDTO> createPlaylist(@Valid @RequestBody PlaylistRequest request) {
        return new ResponseEntity<>(playlistService.createPlaylist(request), HttpStatus.CREATED);
    }

    @GetMapping("/my-playlists")
    @Operation(summary = "Lấy danh sách Playlist cá nhân", description = "Hiển thị toàn bộ playlist thuộc sở hữu của người dùng hiện tại")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    public ResponseEntity<List<PlaylistResponseDTO>> getMyPlaylists() {
        return ResponseEntity.ok(playlistService.getMyPlaylists());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin Playlist", description = "Thay đổi tên hoặc trạng thái của playlist dựa theo ID")
    @ApiResponse(responseCode = "200", description = "Cập nhật playlist thành công")
    public ResponseEntity<PlaylistResponseDTO> updatePlaylist(
            @Parameter(description = "ID của Playlist cần cập nhật", example = "1") @PathVariable Integer id,
            @Valid @RequestBody PlaylistRequest request) {
        return ResponseEntity.ok(playlistService.updatePlaylist(id, request));
    }

    @PutMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cập nhật hình ảnh Playlist", description = "Tải lên tệp ảnh để làm hình đại diện cho playlist")
    @ApiResponse(responseCode = "200", description = "Cập nhật hình ảnh playlist thành công")
    public ResponseEntity<PlaylistResponseDTO> uploadPlaylistImage(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file) {
        // Lưu ý: Bồ cần bổ sung hàm uploadPlaylistImage(Integer id, MultipartFile file) vào trong
        // IPlaylistService và triển khai lưu file + gọi playlistRepository.save() ở PlaylistServiceImpl nhé.
        return ResponseEntity.ok(playlistService.uploadPlaylistImage(id, file));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa Playlist", description = "Xóa hoàn toàn một playlist dựa theo ID")
    @ApiResponse(responseCode = "200", description = "Xóa playlist thành công")
    public ResponseEntity<String> deletePlaylist(
            @Parameter(description = "ID của Playlist cần xóa", example = "1") @PathVariable Integer id) {
        playlistService.deletePlaylist(id);
        return ResponseEntity.ok("Xóa playlist thành công!");
    }

    @PostMapping("/{id}/tracks")
    @Operation(summary = "Thêm bài hát vào Playlist", description = "Thêm một bài hát mới vào danh sách phát dựa theo ID bài hát và ID playlist")
    @ApiResponse(responseCode = "200", description = "Thêm bài hát vào playlist thành công")
    public ResponseEntity<String> addTrackToPlaylist(
            @Parameter(description = "ID của Playlist", example = "1") @PathVariable Integer id,
            @Valid @RequestBody AddTrackRequest request) {
        playlistService.addTrackToPlaylist(id, request);
        return ResponseEntity.ok("Thêm bài hát vào playlist thành công!");
    }

    @DeleteMapping("/{id}/tracks/{trackId}")
    @Operation(summary = "Xóa bài hát khỏi Playlist", description = "Loại bỏ một bài hát cụ thể ra khỏi playlist")
    @ApiResponse(responseCode = "200", description = "Đã loại bỏ bài hát khỏi playlist")
    public ResponseEntity<String> removeTrackFromPlaylist(
            @Parameter(description = "ID của Playlist", example = "1") @PathVariable Integer id,
            @Parameter(description = "ID của Bài hát cần loại bỏ", example = "5") @PathVariable Integer trackId) {
        playlistService.removeTrackFromPlaylist(id, trackId);
        return ResponseEntity.ok("Đã loại bỏ bài hát khỏi playlist.");
    }
}