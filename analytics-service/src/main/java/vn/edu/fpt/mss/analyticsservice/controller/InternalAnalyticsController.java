package vn.edu.fpt.mss.analyticsservice.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.mss.analyticsservice.dto.response.AnalyticsStatsResponse;
import vn.edu.fpt.mss.analyticsservice.service.StatisticsService;
import vn.edu.fpt.mss.analyticsservice.service.TrackingService;

import java.util.Map;

@Hidden
@RestController
@RequestMapping("/internal/history")
@RequiredArgsConstructor
public class InternalAnalyticsController {

    private final StatisticsService statisticsService;
    private final TrackingService trackingService;

    @GetMapping("/stats")
    public AnalyticsStatsResponse stats() {
        return statisticsService.stats();
    }

    @DeleteMapping("/cleanup")
    public Map<String, Integer> cleanup(@RequestParam(defaultValue = "90") int days) {
        return Map.of("deleted", trackingService.cleanupOlderThan(days));
    }
}
