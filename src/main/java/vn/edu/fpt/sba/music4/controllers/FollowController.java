package vn.edu.fpt.sba.music4.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.services.IFollowService;

import java.util.Map;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FollowController {

    private final IFollowService followService;

    @PostMapping("/toggle")
    public ResponseEntity<?> toggleFollow(@RequestParam Integer userId, @RequestParam Integer artistId) {
        boolean isFollowing = followService.toggleFollow(userId, artistId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "following", isFollowing,
                "message", isFollowing ? "Đã theo dõi nghệ sĩ!" : "Đã hủy theo dõi nghệ sĩ!"
        ));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getMyFollowings(@PathVariable Integer userId) {
        return ResponseEntity.ok(followService.getMyFollowings(userId));
    }
}