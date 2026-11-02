package vn.edu.fpt.mss.engagementservice.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.mss.engagementservice.dto.response.ArtistEngagementStats;
import vn.edu.fpt.mss.engagementservice.dto.response.EngagementStatsResponse;
import vn.edu.fpt.mss.engagementservice.dto.response.UserSignalsResponse;
import vn.edu.fpt.mss.engagementservice.service.InsightService;

import java.util.List;

@Hidden
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalEngagementController {

    private final InsightService insightService;

    /** analytics gọi 1 lần để lấy toàn bộ tín hiệu gợi ý của user. */
    @GetMapping("/users/{userId}/signals")
    public UserSignalsResponse signals(@PathVariable Integer userId) {
        return insightService.signals(userId);
    }

    @GetMapping("/artists/{artistId}/stats")
    public ArtistEngagementStats artistStats(@PathVariable Integer artistId,
                                             @RequestParam(required = false) List<Integer> trackIds) {
        return insightService.artistStats(artistId, trackIds);
    }

    @GetMapping("/stats")
    public EngagementStatsResponse stats() {
        return insightService.stats();
    }
}
