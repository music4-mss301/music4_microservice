package vn.edu.fpt.sba.music4.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.sba.music4.dtos.TrackUploadDTO;
import vn.edu.fpt.sba.music4.entities.Track;
import vn.edu.fpt.sba.music4.services.ITrackService;

import java.util.List;

@RestController
@RequestMapping("/api/tracks")
@Tag(name = "Bài hát (Track API)", description = "Các API phục vụ luồng Upload, cập nhật thông tin và quản lý bài hát")
public class  TrackController {
    private final ITrackService trackService;

    public TrackController(ITrackService trackService) {
        this.trackService = trackService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload bài hát mới kèm file", description = "Nhận vào file âm thanh, ảnh bìa và thông tin DTO dưới dạng Multipart Form-Data")
    @ApiResponse(responseCode = "200", description = "Upload bài hát và lưu thông tin thành công")
    @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào hoặc file không hợp lệ")
    public ResponseEntity<String> uploadTrack(
            @RequestPart("audioFile") MultipartFile audioFile,
            @RequestPart("coverImage") MultipartFile coverImage,
            @ModelAttribute TrackUploadDTO dto) {
        trackService.uploadTrack(audioFile, coverImage, dto);
        return ResponseEntity.ok("Upload bài hát và lưu thông tin thành công!");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết bài hát theo ID", description = "Trả về toàn bộ thông tin chi tiết của một bài hát")
    @ApiResponse(responseCode = "200", description = "Tìm thấy bài hát thành công")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy bài hát với ID tương ứng")
    public ResponseEntity<Track> getTrackById(
            @Parameter(description = "ID của bài hát cần tìm", example = "1") @PathVariable Integer id) {
        return ResponseEntity.ok(trackService.getTrackById(id));
    }

    @GetMapping("/album/{albumId}")
    @Operation(summary = "Lấy danh sách bài hát thuộc Album", description = "Trả về toàn bộ danh sách các bài hát nằm trong một Album cụ thể")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    public ResponseEntity<List<Track>> getTracksByAlbumId(
            @Parameter(description = "ID của Album cần bốc bài hát", example = "1") @PathVariable Integer albumId) {
        return ResponseEntity.ok(trackService.getTracksByAlbumId(albumId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin (Metadata) bài hát", description = "Sửa tên, thời lượng, danh sách ca sĩ collab hoặc thể loại của bài hát")
    @ApiResponse(responseCode = "200", description = "Cập nhật thông tin bài hát thành công")
    public ResponseEntity<Track> updateTrackMetadata(
            @Parameter(description = "ID bài hát cần cập nhật", example = "1") @PathVariable Integer id,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Thông tin cập nhật mới của bài hát", required = true)
            @org.springframework.web.bind.annotation.RequestBody TrackUploadDTO dto) {

        return ResponseEntity.ok(trackService.updateTrackMetadata(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa bài hát hoàn toàn", description = "Xóa bài hát khỏi hệ thống và tự động dọn sạch liên kết ở các bảng trung gian")
    @ApiResponse(responseCode = "200", description = "Xóa bài hát thành công")
    public ResponseEntity<String> deleteTrack(
            @Parameter(description = "ID bài hát cần xóa", example = "1") @PathVariable Integer id) {
        trackService.deleteTrack(id);
        return ResponseEntity.ok("Xóa bài hát hoàn toàn thành công!");
    }
}