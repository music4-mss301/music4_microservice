package vn.edu.fpt.sba.music4.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.request.LoginRequest;
import vn.edu.fpt.sba.music4.dtos.request.RefreshRequest;
import vn.edu.fpt.sba.music4.dtos.request.RegistrationRequest;
import vn.edu.fpt.sba.music4.dtos.response.AuthResponse;
import vn.edu.fpt.sba.music4.dtos.response.TokenResponse;
import vn.edu.fpt.sba.music4.entities.Artist;
import vn.edu.fpt.sba.music4.entities.RefreshToken;
import vn.edu.fpt.sba.music4.enums.ERole;
import vn.edu.fpt.sba.music4.services.impl.ArtistServiceImpl;
import vn.edu.fpt.sba.music4.services.impl.RefreshTokenServiceImpl;
import vn.edu.fpt.sba.music4.util.EnumUtil;
import vn.edu.fpt.sba.music4.util.JwtUtil;
import vn.edu.fpt.sba.music4.services.impl.CustomUserDetailsServiceImpl;
import vn.edu.fpt.sba.music4.services.impl.UserServiceImpl;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsServiceImpl userDetailsService;
    private final UserServiceImpl userService;
    private final ArtistServiceImpl artistService;
    private final RefreshTokenServiceImpl refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest) {
//        try {
            // 1. Xác thực thông tin username và password
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.username(),
                            loginRequest.password()));

            UserDetails user = (UserDetails) authentication.getPrincipal();
            String source = (user instanceof Artist) ? "artist" : "user";
            String accessToken = jwtUtil.generateToken(user);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername(), source);
            return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken.getToken()));

//        } catch (BadCredentialsException e) {
//            // Sai mật khẩu/tài khoản -> Trả về 401 Unauthorized kèm thông báo
////            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse("Invalid username or password"));
//        } catch (Exception e) {
//            // Các lỗi hệ thống khác (ví dụ lỗi JWT) -> In ra console để bạn debug
//            e.printStackTrace();
////            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthResponse("An error occurred during authentication"));
//        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshRequest req) {
        RefreshToken rt   = refreshTokenService.verifyRefreshToken(req.refreshToken());
        UserDetails user  = userDetailsService.loadUserByUsername(rt.getUsername());
        String newAccess  = jwtUtil.generateToken(user);
        RefreshToken newRt = refreshTokenService.rotate(rt);         // token rotation

        return ResponseEntity.ok(new TokenResponse(newAccess, newRt.getToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest req) {
        refreshTokenService.revokeByToken(req.refreshToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestParam("role") String role ,@RequestBody RegistrationRequest registrationRequest) {
        if (role == null || role.isEmpty()) {
            return ResponseEntity.badRequest().body(new AuthResponse("Role parameter is required"));
        }

        switch (role.toLowerCase()) {
            case "artist" -> {
                try {
                    artistService.save(registrationRequest);
                    return ResponseEntity.ok(new AuthResponse("Artist registered successfully"));
                } catch (IllegalArgumentException ex) {
                    return ResponseEntity.badRequest().body(new AuthResponse(ex.getMessage()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthResponse("An error occurred while registering artist"));
                }
            }
            case "listener" -> {
                try{
                    userService.save(registrationRequest);
                    return ResponseEntity.ok(new AuthResponse("User registered successfully"));
                }  catch (IllegalArgumentException ex) {
                    return ResponseEntity.badRequest().body(new AuthResponse(ex.getMessage()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthResponse("An error occurred while registering user"));
                }
            }
            default -> {
                return ResponseEntity.badRequest().body(new AuthResponse("Invalid role specified. Must be 'artist' or 'user'."));
            }
        }
    }

    @GetMapping("/test")
    public ResponseEntity<AuthResponse> test() {
        return ResponseEntity.ok(new AuthResponse("test"));
    }
}
