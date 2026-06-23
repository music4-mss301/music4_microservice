package vn.edu.fpt.sba.music4.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.sba.music4.dtos.TrackUploadDTO;
import vn.edu.fpt.sba.music4.dtos.response.AlbumBulkJsonDTO;
import vn.edu.fpt.sba.music4.dtos.response.TrackDetailResponseDTO;
import vn.edu.fpt.sba.music4.dtos.response.TrackResponseDTO;
import vn.edu.fpt.sba.music4.entities.Track;
import vn.edu.fpt.sba.music4.services.ITrackService;
import vn.edu.fpt.sba.music4.services.impl.TrackServiceImpl;
import vn.edu.fpt.sba.music4.util.AWSUtil;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tracks")
@Tag(name = "Bài hát (Track API)", description = "Các API phục vụ luồng Upload, cập nhật thông tin và quản lý bài hát")
public class TrackController {
    private final ITrackService trackService;
    private final AWSUtil awsUtil;

    public TrackController(ITrackService trackService, AWSUtil awsUtil) {
        this.trackService = trackService;
        this.awsUtil = awsUtil;
    }

    // ok
    @GetMapping("")
    @Operation(summary = "Lấy tất cả bài hát", description = "Trả về toàn bộ danh sách các bài hát")
    public List<TrackDetailResponseDTO> getAllTrack() {
        return trackService.getAllTrack();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload bài hát mới kèm file", description = "Nhận vào file âm thanh, ảnh bìa và thông tin DTO dưới dạng Multipart Form-Data")
    @ApiResponse(responseCode = "200", description = "Upload bài hát và lưu thông tin thành công")
    @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào hoặc file không hợp lệ")
    public ResponseEntity<TrackDetailResponseDTO> uploadTrack(
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam("coverImage") MultipartFile coverImage,
            @ModelAttribute @Valid TrackUploadDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String currentUsername = userDetails.getUsername(); // Đây là username/email của ca sĩ
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("Lỗi: File âm thanh (audioFile) không được để trống!");
        }
        if (coverImage == null || coverImage.isEmpty()) {
            throw new IllegalArgumentException("Lỗi: File ảnh bìa (coverImage) không được để trống!");
        }
        TrackDetailResponseDTO response = trackService.uploadTrack(audioFile, coverImage, dto, currentUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ok
    @CrossOrigin(origins = "http://localhost:5173")
    @PostMapping(value = "/upload-temp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file tạm thời lên S3", description = "Đẩy file lẻ lên S3 trước để lấy S3 Key và thời lượng nhạc (nếu là tracks)")
    public ResponseEntity<?> uploadTempFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type) { // type: "images" hoặc "tracks"

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File upload không được để trống!"));
        }

        if (!"images".equalsIgnoreCase(type) && !"tracks".equalsIgnoreCase(type)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Type không hợp lệ! Chỉ chấp nhận 'images' hoặc 'tracks'."));
        }

        try {
            // 1. Đẩy file lên S3 (Hàm này bắn ra IOException nên cần nằm trong khối try)
            String s3Key = awsUtil.uploadFile(file, type);

            Map<String, Object> response = new HashMap<>();
            response.put("s3Key", s3Key);

            // 2. Nếu là file nhạc, bóc tách luôn thời lượng để trả về cho Frontend
            if ("tracks".equalsIgnoreCase(type)) {
                Integer duration = trackService.getAudioDurationInSeconds(file);
                response.put("duration", duration);
            }

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            // Bắt lỗi hệ thống khi đọc InputStream của file hoặc lỗi đường truyền mạng với S3
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi đọc ghi file hoặc kết nối S3 thất bại: " + e.getMessage()));
        } catch (Exception e) {
            // Bắt các lỗi phát sinh khác (ví dụ: lỗi thư viện khi đọc duration bài hát)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Xử lý file thất bại do: " + e.getMessage()));
        }
    }

    // ok
    @PostMapping("/bulk-json")
    @Operation(summary = "Lưu hàng loạt bài hát bằng JSON", description = "Nhận vào metadata JSON đã có sẵn link S3 từ bước upload trước")
    public ResponseEntity<String> saveBulkTracksJson(@RequestBody AlbumBulkJsonDTO dto) {
        trackService.saveBulkTracksJson(dto);
        return ResponseEntity.ok("Phát hành Album thành công!");
    }

    // ok
    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết bài hát theo ID", description = "Trả về toàn bộ thông tin chi tiết của một bài hát")
    @ApiResponse(responseCode = "200", description = "Tìm thấy bài hát thành công")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy bài hát với ID tương ứng")
    public ResponseEntity<TrackDetailResponseDTO> getTrackById(
            @Parameter(description = "ID của bài hát cần tìm", example = "1") @PathVariable Integer id) {
        return ResponseEntity.ok(trackService.getTrackById(id));
    }

    // ok
    @GetMapping("/album/{albumId}")
    @Operation(summary = "Lấy danh sách bài hát thuộc Album", description = "Trả về toàn bộ danh sách các bài hát nằm trong một Album cụ thể")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    public ResponseEntity<List<TrackResponseDTO>> getTracksByAlbumId(
            @Parameter(description = "ID của Album cần bốc bài hát", example = "1") @PathVariable Integer albumId) {
        return ResponseEntity.ok(trackService.getTracksByAlbumId(albumId));
    }

    // ok
    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin (Metadata) bài hát", description = "Sửa tên, thời lượng, danh sách ca sĩ collab hoặc thể loại của bài hát")
    @ApiResponse(responseCode = "200", description = "Cập nhật thông tin bài hát thành công")
    public ResponseEntity<TrackDetailResponseDTO> updateTrackMetadata(
            @Parameter(description = "ID bài hát cần cập nhật", example = "1") @PathVariable Integer id,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Thông tin cập nhật mới của bài hát", required = true)
            @org.springframework.web.bind.annotation.RequestBody TrackUploadDTO dto) {

        return ResponseEntity.ok(trackService.updateTrackMetadata(id, dto));
    }

    // ok
    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa bài hát hoàn toàn", description = "Xóa bài hát khỏi hệ thống và tự động dọn sạch liên kết ở các bảng trung gian")
    @ApiResponse(responseCode = "200", description = "Xóa bài hát thành công")
    public ResponseEntity<String> deleteTrack(
            @Parameter(description = "ID bài hát cần xóa", example = "1") @PathVariable Integer id) {
        trackService.deleteTrack(id);
        return ResponseEntity.ok("Xóa bài hát có " + id + " thành công!");
    }

    // ok
    @GetMapping("/stream/{id}")
    @Operation(summary = "Stream nhạc trực tuyến (Hỗ trợ tua)", description = "Trả về luồng byte âm thanh (HTTP 206). Hỗ trợ nhận Header Range từ client để tua nhạc.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "206", description = "Đang stream phân đoạn âm thanh (Partial Content)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bài hát")
    })
    public ResponseEntity<StreamingResponseBody> streamTrack(
            @Parameter(description = "ID bài hát cần stream", example = "1") @PathVariable Integer id,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
        // 1. Lấy thông tin bài hát từ DB
        TrackDetailResponseDTO track = trackService.getTrackById(id);
        // Lưu ý: Dùng track.filePath() thay vì track.getFilePath() vì DTO là Java Record
        String s3Key = track.filePath();
        if (s3Key == null || s3Key.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        // 2. Gọi sang AWSUtil để xử lý lấy luồng byte từ S3
        return awsUtil.streamAudioFile(s3Key, rangeHeader);
    }

    // ok
    @GetMapping("/stream/preview/{id}")
    @Operation(summary = "Nghe thử 30s (Guest)", description = "Stream một đoạn nhạc giới hạn dung lượng đầu tiên, đánh lừa trình duyệt đây là toàn bộ bài hát.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "206", description = "Đang stream phân đoạn âm thanh Preview"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bài hát")
    })
    public ResponseEntity<StreamingResponseBody> streamPreviewTrack(
            @Parameter(description = "ID bài hát cần stream", example = "1") @PathVariable Integer id,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {

        // 1. Lấy thông tin bài hát từ DB
        TrackDetailResponseDTO track = trackService.getTrackById(id);

        // 2. Ưu tiên lấy previewPath (Mặc dù hiện tại nó đang giống filePath, nhưng chuẩn logic là vậy)
        String rawPath = track.previewPath();

        if (rawPath == null || rawPath.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String s3Key = awsUtil.extractKeyFromUrl(rawPath);

        // 3. Gọi sang AWSUtil xử lý luồng Preview (Cách ly với luồng nghe Full)
        return awsUtil.streamPreviewAudioFile(s3Key, rangeHeader);
    }


    @GetMapping("/top5-views")
    @Operation(summary = "Lấy Top 5 bài hát nhiều lượt xem nhất", description = "Trả về danh sách 5 bài hát có lượt view cao nhất toàn hệ thống")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    public ResponseEntity<List<TrackDetailResponseDTO>> getTop5MostViewedTracks() {

        return ResponseEntity.ok(trackService.getTop5MostViewedTracks());
    }

    // ok
    @GetMapping("/artist/{artistId}")
    @Operation(summary = "Lấy danh sách bài hát của một ca sĩ", description = "Trả về toàn bộ danh sách các bài hát thuộc về một nghệ sĩ cụ thể")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    public ResponseEntity<List<TrackResponseDTO>> getTracksByArtistId(
            @Parameter(description = "ID của ca sĩ cần lấy bài hát", example = "1") @PathVariable Integer artistId) {

        List<TrackResponseDTO> tracks = trackService.getTracksByArtistId(artistId);
        return ResponseEntity.ok(tracks);
    }
}