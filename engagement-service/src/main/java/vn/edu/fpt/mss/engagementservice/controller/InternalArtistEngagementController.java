package vn.edu.fpt.mss.engagementservice.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.mss.engagementservice.dto.request.ArtistEngagementQuery;
import vn.edu.fpt.mss.engagementservice.dto.response.ArtistEngagementResult;
import vn.edu.fpt.mss.engagementservice.service.ArtistFollowerService;

import java.util.List;

@Hidden
@RestController
@RequestMapping("/internal/artists")
@RequiredArgsConstructor
public class InternalArtistEngagementController {

    private final ArtistFollowerService service;

    @PostMapping("/engagement")
    public List<ArtistEngagementResult> engagement(@RequestBody List<ArtistEngagementQuery> queries) {
        return service.engagementOf(queries);
    }
}
