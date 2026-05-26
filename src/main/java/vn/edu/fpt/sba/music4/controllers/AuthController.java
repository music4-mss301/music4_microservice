package vn.edu.fpt.sba.music4.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.sba.music4.dtos.request.LoginRequest;
import vn.edu.fpt.sba.music4.dtos.response.AuthResponse;
import vn.edu.fpt.sba.music4.services.JwtService;
import vn.edu.fpt.sba.music4.services.impl.CustomUserDetailsServiceImpl;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final CustomUserDetailsServiceImpl userDetailsService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            // 1. Xác thực thông tin username và password
            // Nếu sai, dòng này sẽ ném ra BadCredentialsException
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));


//            UserDetails user = this.userDetailsService.loadUserByUsername(loginRequest.username());
            UserDetails user = (UserDetails) authentication.getPrincipal();

            String token = jwtService.generateToken(user);

            System.out.println(token);
            return ResponseEntity.ok(new AuthResponse(token));

        } catch (BadCredentialsException e) {
            // Sai mật khẩu/tài khoản -> Trả về 401 Unauthorized kèm thông báo
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse("Invalid username or password"));
        } catch (Exception e) {
            // Các lỗi hệ thống khác (ví dụ lỗi JWT) -> In ra console để bạn debug
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthResponse("An error occurred during authentication"));
        }
    }

    @GetMapping("/test")
    public ResponseEntity<AuthResponse> test() {
        return ResponseEntity.ok(new AuthResponse("test"));
    }
}
