package vn.edu.fpt.mss.engagementservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.mss.engagementservice.dto.client.UserSummary;
import vn.edu.fpt.mss.engagementservice.service.ArtistFollowerService;

import java.util.List;

/** Gateway chuyển /api/artists/{id}/followers về service này (route riêng, đứng trước route của content). */
@Tag(name = "Follows")
@RestController
@RequestMapping("/api/artists")
@RequiredArgsConstructor
public class ArtistFollowersController {

    private final ArtistFollowerService service;

    @GetMapping("/{id}/followers")
    public List<UserSummary> followers(@PathVariable Integer id) {
        return service.followersOf(id);
    }
}
