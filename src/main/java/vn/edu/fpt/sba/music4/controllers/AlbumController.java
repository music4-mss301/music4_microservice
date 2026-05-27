package vn.edu.fpt.sba.music4.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping("/artist/{artistId}")
    @Operation(summary = "Lấy danh sách Album của một ca sĩ", description = "Lọc và hiển thị toàn bộ album do nghệ sĩ đó sở hữu")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    public ResponseEntity<List<Album>> getAlbumsByArtist(
            @Parameter(description = "ID của Nghệ sĩ", example = "1") @PathVariable Integer artistId) {
        return ResponseEntity.ok(albumService.getAlbumsByArtist(artistId));
    }

    @PostMapping
    @Operation(summary = "Tạo Album mới", description = "Tạo một album trống cho nghệ sĩ trước khi upload bài hát")
    @ApiResponse(responseCode = "200", description = "Tạo album thành công")
    public ResponseEntity<Album> createAlbum(@RequestBody Album album) {
        return ResponseEntity.ok(albumService.createAlbum(album));
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
