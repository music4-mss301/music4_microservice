package vn.edu.fpt.mss.engagementservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.mss.engagementservice.dto.request.AddTrackRequest;
import vn.edu.fpt.mss.engagementservice.dto.request.PlaylistImageRequest;
import vn.edu.fpt.mss.engagementservice.dto.request.PlaylistRequest;
import vn.edu.fpt.mss.engagementservice.dto.response.PlaylistDetailResponse;
import vn.edu.fpt.mss.engagementservice.dto.response.PlaylistResponse;
import vn.edu.fpt.mss.engagementservice.dto.response.PlaylistTrackItem;
import vn.edu.fpt.mss.engagementservice.service.PlaylistService;

import java.util.List;

@Tag(name = "Playlists")
@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    @GetMapping({"/my-playlists", "/me"})
    public List<PlaylistResponse> mine() {
        return playlistService.getMine();
    }

    @GetMapping("/user/{userId}")
    public List<PlaylistResponse> byUser(@PathVariable Integer userId) {
        return playlistService.getByUser(userId);
    }

    @GetMapping("/{id}")
    public PlaylistDetailResponse detail(@PathVariable Integer id) {
        return playlistService.getDetail(id);
    }

    @GetMapping("/{id}/tracks")
    public List<PlaylistTrackItem> tracks(@PathVariable Integer id) {
        return playlistService.getDetail(id).tracks();
    }

    @PostMapping
    public ResponseEntity<PlaylistResponse> create(@Valid @RequestBody PlaylistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playlistService.create(request));
    }

    @PutMapping("/{id}")
    public PlaylistResponse update(@PathVariable Integer id, @Valid @RequestBody PlaylistRequest request) {
        return playlistService.update(id, request);
    }

    @PutMapping("/{id}/image")
    public PlaylistResponse updateImage(@PathVariable Integer id, @Valid @RequestBody PlaylistImageRequest request) {
        return playlistService.updateImage(id, request.img());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        playlistService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/tracks")
    public ResponseEntity<PlaylistDetailResponse> addTrack(@PathVariable Integer id,
                                                           @Valid @RequestBody AddTrackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playlistService.addTrack(id, request.trackId()));
    }

    @DeleteMapping("/{id}/tracks/{trackId}")
    public ResponseEntity<Void> removeTrack(@PathVariable Integer id, @PathVariable Integer trackId) {
        playlistService.removeTrack(id, trackId);
        return ResponseEntity.noContent().build();
    }
}
