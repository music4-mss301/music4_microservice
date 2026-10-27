package vn.edu.fpt.mss.contentservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.mss.contentservice.dto.response.*;
import vn.edu.fpt.mss.contentservice.service.FrontendCompatService;

import java.util.List;

/** Endpoint của monolith mà FE đang dùng (trang chủ, khám phá ngẫu nhiên, dashboard admin). Công khai (GET). */
@Tag(name = "Frontend compat")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FrontendCompatController {

    private final FrontendCompatService service;

    @GetMapping("/tracks/top5-views")
    public List<TrackResponse> top5Views() {
        return service.top5MostViewed();
    }

    @GetMapping("/tracks/random")
    public PageResponse<TrackResponse> random(@RequestParam int seed,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return service.randomTracks(seed, page, size);
    }

    @GetMapping("/artists/count")
    public long artistCount() {
        return service.countArtists();
    }

    @GetMapping("/artists/top3")
    public List<TopArtistResponse> top3Artists() {
        return service.top3Artists();
    }

    @GetMapping("/categories/stats")
    public List<CategoryStatResponse> categoryStats() {
        return service.categoryStats();
    }

    @GetMapping("/categories/{id}/tracks")
    public PageResponse<TrackResponse> tracksOfCategory(@PathVariable Integer id,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "12") int size) {
        return service.tracksOfCategory(id, page, size);
    }

    @GetMapping("/albums/all")
    public PageResponse<AlbumResponse> allAlbums(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "12") int size) {
        return service.allAlbums(page, size);
    }
}
