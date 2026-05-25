package vn.edu.fpt.sba.music4.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.request.SyncTimeRequest;
import vn.edu.fpt.sba.music4.dtos.request.TrackPlayRequest;
import vn.edu.fpt.sba.music4.services.ITrackingService;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
@CrossOrigin("*")
public class TrackingController {

    private final ITrackingService trackingService;

    @PostMapping("/play")
    public ResponseEntity<String> recordPlayHistory(@Valid @RequestBody TrackPlayRequest request) {
        trackingService.recordPlayHistory(request.trackId(), request.userId());

        return ResponseEntity.ok("Listen recorded successfully!");
    }

    @PutMapping("/sync-time")
    public ResponseEntity<String> syncPlaybackTime(@Valid @RequestBody SyncTimeRequest request) {
        trackingService.syncPlaybackPosition(request);
        return ResponseEntity.ok("Time synchronized successfully!");
    }
}