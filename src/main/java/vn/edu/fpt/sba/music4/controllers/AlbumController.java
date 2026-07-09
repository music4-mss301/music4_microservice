package vn.edu.fpt.sba.music4.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.response.AlbumCreateDTO;
import vn.edu.fpt.sba.music4.dtos.response.AlbumDetailResponseDTO;
import vn.edu.fpt.sba.music4.dtos.response.AlbumResponseDTO;
import vn.edu.fpt.sba.music4.dtos.response.TrackResponseDTO;
import vn.edu.fpt.sba.music4.entities.Album;
import vn.edu.fpt.sba.music4.services.IAlbumService;

import java.util.List;

@RestController
@RequestMapping("/api/albums")
@Tag(name = "Album API", description = "Quản lý dữ liệu Album của nghệ sĩ")
public class AlbumController {
    private final IAlbumService albumService;

    public AlbumController(IAlbumService albumService) {
        this.albumService = albumService;
    }

    // ok
    @GetMapping("")
    @Operation(summary = "Lấy danh sách Album", description = "Hiển thị toàn bộ album có trong hệ thống")
    public ResponseEntity<List<AlbumResponseDTO>> getAllAlbums() {
        return ResponseEntity.ok(albumService.findAll());
    }

    @GetMapping("/all")
    @Operation(summary = "Lấy danh sách Album", description = "Hiển thị toàn bộ album có trong hệ thống")
    public Page<AlbumDetailResponseDTO> getAllAlbums(@RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return albumService.findAll(pageable);
    }

    // ok
    @GetMapping("/artist/{artistId}")
    @Operation(summary = "Lấy danh sách Album của một ca sĩ", description = "Lọc và hiển thị toàn bộ album do nghệ sĩ đó sở hữu")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    public ResponseEntity<List<AlbumResponseDTO>> getAlbumsByArtist(
            @Parameter(description = "ID của Nghệ sĩ", example = "1") @PathVariable Integer artistId) {
        return ResponseEntity.ok(albumService.getAlbumsByArtist(artistId));
    }

    // ok
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // Định nghĩa API này nhận form-data
    @Operation(summary = "Tạo Album mới", description = "Tạo một album kèm ảnh bìa cho nghệ sĩ")
    @ApiResponse(responseCode = "201", description = "Tạo album thành công")
    public ResponseEntity<Album> createAlbum(@Valid @ModelAttribute AlbumCreateDTO album) { // 🔥 ĐỔI THÀNH @ModelAttribute
        return new ResponseEntity<>(albumService.createAlbum(album), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa Album hoàn toàn", description = "Xóa album dựa theo ID được chỉ định")
    @ApiResponse(responseCode = "200", description = "Xóa album thành công")
    public ResponseEntity<String> deleteAlbum(
            @Parameter(description = "ID của Album cần xóa", example = "1") @PathVariable Integer id) {
        albumService.deleteAlbum(id);
        return ResponseEntity.ok("Xóa album thành công!");
    }
}
