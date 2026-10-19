package vn.edu.fpt.mss.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.mss.userservice.dto.request.*;
import vn.edu.fpt.mss.userservice.dto.response.MessageResponse;
import vn.edu.fpt.mss.userservice.dto.response.TokenResponse;
import vn.edu.fpt.mss.userservice.security.SecurityUtils;
import vn.edu.fpt.mss.userservice.service.AuthService;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Đăng ký (role=listener|artist)")
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@RequestParam(defaultValue = "listener") String role,
                                                    @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, role));
    }

    @Operation(summary = "Đăng nhập bằng username hoặc email")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Làm mới access token (rotation refresh token)")
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return new MessageResponse("Nếu email tồn tại, link đặt lại mật khẩu đã được gửi");
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return new MessageResponse("Đặt lại mật khẩu thành công");
    }

    @PutMapping("/change-password")
    public MessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(SecurityUtils.requireUser().id(), request);
        return new MessageResponse("Đổi mật khẩu thành công, vui lòng đăng nhập lại trên các thiết bị khác");
    }
}
