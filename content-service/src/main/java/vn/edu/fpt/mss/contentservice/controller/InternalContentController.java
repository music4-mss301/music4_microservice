package vn.edu.fpt.mss.contentservice.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.mss.contentservice.dto.event.TrackEventPayload;
import vn.edu.fpt.mss.contentservice.dto.request.CreateArtistProfileRequest;
import vn.edu.fpt.mss.contentservice.dto.response.*;
import vn.edu.fpt.mss.contentservice.mapper.ContentMapper;
import vn.edu.fpt.mss.contentservice.service.ArtistService;
import vn.edu.fpt.mss.contentservice.service.TrackService;

import java.util.List;

/** API nội bộ cho service khác (X-Internal-Token). Gateway không route /internal/**. */
@Hidden
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalContentController {

    private final TrackService trackService;
    private final ArtistService artistService;

    @GetMapping("/tracks")
    public List<TrackResponse> byIds(@RequestParam List<Integer> ids) {
        return trackService.getByIds(ids);
    }

    @GetMapping("/tracks/export")
    public PageResponse<TrackEventPayload> export(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "100") int size) {
        return trackService.exportForIndex(page, size);
    }

    @GetMapping("/tracks/search")
    public PageResponse<TrackResponse> search(@RequestParam String keyword,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return trackService.search(keyword, page, size);
    }

    @GetMapping("/tracks/trending")
    public List<TrackResponse> trending(@RequestParam(defaultValue = "10") int limit) {
        return trackService.getTrending(limit);
    }

    @GetMapping("/artists")
    public List<ArtistResponse> artists(@RequestParam List<Integer> ids) {
        return artistService.getByIds(ids);
    }

    @GetMapping("/artists/{id}")
    public ArtistResponse artist(@PathVariable Integer id) {
        return ContentMapper.toArtistResponse(artistService.findActive(id));
    }

    @GetMapping("/artists/{id}/stats")
    public ArtistStatsResponse artistStats(@PathVariable Integer id) {
        return artistService.stats(id);
    }

    @PostMapping("/artists")
    public ArtistResponse createArtistProfile(@Valid @RequestBody CreateArtistProfileRequest request) {
        return artistService.createProfile(request);
    }

    @GetMapping("/stats")
    public ContentStatsResponse stats() {
        return trackService.stats();
    }
}
