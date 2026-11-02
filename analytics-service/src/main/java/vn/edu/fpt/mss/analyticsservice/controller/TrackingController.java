package vn.edu.fpt.mss.analyticsservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.mss.analyticsservice.dto.request.PlayRequest;
import vn.edu.fpt.mss.analyticsservice.dto.request.SyncTimeRequest;
import vn.edu.fpt.mss.analyticsservice.dto.response.HistoryResponse;
import vn.edu.fpt.mss.analyticsservice.dto.response.PageResponse;
import vn.edu.fpt.mss.analyticsservice.dto.response.PlayResponse;
import vn.edu.fpt.mss.analyticsservice.service.TrackingService;

import java.util.Map;

@Tag(name = "Tracking & History")
@RestController
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    /** Khách cũng ghi được lượt nghe (userId = null). FE cũ gọi cả /tracking/play và /tracking/history. */
    @PostMapping({"/api/tracking/play", "/api/tracking/history"})
    public PlayResponse play(@Valid @RequestBody PlayRequest request) {
        return trackingService.recordPlay(request.trackId());
    }

    @PutMapping("/api/tracking/sync-time")
    public Map<String, String> syncTime(@Valid @RequestBody SyncTimeRequest request) {
        trackingService.syncPosition(request);
        return Map.of("message", "Đã đồng bộ vị trí phát");
    }

    @GetMapping("/api/history/me")
    public PageResponse<HistoryResponse> myHistory(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return trackingService.myHistory(page, size);
    }

    @DeleteMapping("/api/history/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        trackingService.deleteEntry(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/history/me")
    public Map<String, Integer> clearMine() {
        return Map.of("deleted", trackingService.clearMine());
    }
}
