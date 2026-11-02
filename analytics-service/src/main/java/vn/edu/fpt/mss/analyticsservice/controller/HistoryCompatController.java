package vn.edu.fpt.mss.analyticsservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.mss.analyticsservice.dto.client.TrackSummary;
import vn.edu.fpt.mss.analyticsservice.dto.response.PageResponse;
import vn.edu.fpt.mss.analyticsservice.service.HistoryCompatService;

@Tag(name = "Frontend compat")
@RestController
@RequiredArgsConstructor
public class HistoryCompatController {

    private final HistoryCompatService service;

    @GetMapping("/api/tracking/history/{userId}")
    public PageResponse<TrackSummary> history(@PathVariable Integer userId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return service.historyTracks(userId, page, size);
    }
}
