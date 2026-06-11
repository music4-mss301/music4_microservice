package vn.edu.fpt.sba.music4.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.request.AddTrackRequest;
import vn.edu.fpt.sba.music4.dtos.request.PlaylistRequest;
import vn.edu.fpt.sba.music4.entities.Playlist;
import vn.edu.fpt.sba.music4.entities.PlaylistTrack;
import vn.edu.fpt.sba.music4.services.IPlaylistService;

import java.util.List;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PlaylistController {

    private final IPlaylistService playlistService;

    @PostMapping
    public ResponseEntity<Playlist> createPlaylist(@RequestBody PlaylistRequest request) {
        return ResponseEntity.ok(playlistService.createPlaylist(request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<Playlist>> getMyPlaylists() {
        return ResponseEntity.ok(playlistService.getMyPlaylists());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Playlist> updatePlaylist(@PathVariable Integer id, @RequestBody PlaylistRequest request) {
        return ResponseEntity.ok(playlistService.updatePlaylist(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePlaylist(@PathVariable Integer id) {
        playlistService.deletePlaylist(id);
        return ResponseEntity.ok("Xóa playlist thành công!");
    }

    @PostMapping("/{id}/tracks")
    public ResponseEntity<PlaylistTrack> addTrackToPlaylist(
            @PathVariable Integer id,
            @RequestBody AddTrackRequest request) {
        return ResponseEntity.ok(playlistService.addTrackToPlaylist(id, request));
    }

    @DeleteMapping("/{id}/tracks/{trackId}")
    public ResponseEntity<String> removeTrackFromPlaylist(
            @PathVariable Integer id,
            @PathVariable Integer trackId) {
        playlistService.removeTrackFromPlaylist(id, trackId);
        return ResponseEntity.ok("Đã loại bỏ bài hát khỏi playlist.");
    }
}