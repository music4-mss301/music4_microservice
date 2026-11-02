package vn.edu.fpt.mss.userservice.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.mss.userservice.dto.response.UserStatsResponse;
import vn.edu.fpt.mss.userservice.dto.response.UserSummaryResponse;
import vn.edu.fpt.mss.userservice.service.UserService;

import java.util.List;

/** Chỉ cho service khác gọi (X-Internal-Token), gateway không route /internal/**. */
@Hidden
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @GetMapping
    public List<UserSummaryResponse> batch(@RequestParam List<Integer> ids) {
        return userService.getSummaries(ids);
    }

    @GetMapping("/stats")
    public UserStatsResponse stats() {
        return userService.stats();
    }
}
