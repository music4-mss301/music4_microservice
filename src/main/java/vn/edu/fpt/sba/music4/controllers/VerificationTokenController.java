package vn.edu.fpt.sba.music4.controllers;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.sba.music4.dtos.response.AuthResponse;
import vn.edu.fpt.sba.music4.services.impl.VerificationService;

@RestController
@RequestMapping("/api/v1")
public class VerificationTokenController {
    private final VerificationService verificationService;

    public VerificationTokenController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify account email")
    public ResponseEntity<AuthResponse> verifyEmail(@RequestParam("token") String token) {
        try {
            verificationService.verifyAccount(token);
            return ResponseEntity.ok(new AuthResponse("Xác thực tài khoản thành công! Bạn có thể đăng nhập."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new AuthResponse(ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse("Lỗi hệ thống khi xác thực tài khoản."));
        }
    }
}
