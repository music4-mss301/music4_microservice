package vn.edu.fpt.mss.userservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.mss.userservice.service.UserService;

/** Dashboard admin gọi /api/users/count (monolith trả một số nguyên). */
@Tag(name = "Frontend compat")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserCountController {

    private final UserService userService;

    @GetMapping("/count")
    public int totalUsers() {
        return Math.toIntExact(userService.stats().totalUsers());
    }
}
