package vn.edu.fpt.mss.userservice.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.mss.userservice.dto.request.UpdateProfileRequest;
import vn.edu.fpt.mss.userservice.dto.request.UpdateRoleRequest;
import vn.edu.fpt.mss.userservice.dto.request.UpdateStatusRequest;
import vn.edu.fpt.mss.userservice.dto.response.PageResponse;
import vn.edu.fpt.mss.userservice.dto.response.UserResponse;
import vn.edu.fpt.mss.userservice.security.SecurityUtils;
import vn.edu.fpt.mss.userservice.service.UserService;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse me() {
        return userService.getById(SecurityUtils.requireUser().id());
    }

    @PatchMapping("/me")
    public UserResponse updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(SecurityUtils.requireUser().id(), request);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Integer id) {
        return userService.getById(id);
    }

    /** FE đang gọi PATCH /users/{id}: chủ tài khoản hoặc ADMIN. */
    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable Integer id, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public PageResponse<UserResponse> search(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String role,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return userService.search(keyword, role, page, size);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable Integer id, @Valid @RequestBody UpdateRoleRequest request) {
        return userService.changeRole(id, request.role());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public UserResponse changeStatus(@PathVariable Integer id, @Valid @RequestBody UpdateStatusRequest request) {
        return userService.changeStatus(id, request.enabled());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
