package vn.edu.fpt.sba.music4.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.sba.music4.dtos.request.*;
import vn.edu.fpt.sba.music4.dtos.response.AuthResponse;
import vn.edu.fpt.sba.music4.dtos.response.TokenResponse;
import vn.edu.fpt.sba.music4.entities.Artist;
import vn.edu.fpt.sba.music4.entities.RefreshToken;
import vn.edu.fpt.sba.music4.services.IPasswordResetTokenService;
import vn.edu.fpt.sba.music4.services.impl.ArtistServiceImpl;
import vn.edu.fpt.sba.music4.services.impl.RefreshTokenServiceImpl;
import vn.edu.fpt.sba.music4.util.JwtUtil;
import vn.edu.fpt.sba.music4.services.impl.CustomUserDetailsServiceImpl;
import vn.edu.fpt.sba.music4.services.impl.UserServiceImpl;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API endpoints for user authentication, registration, token refresh, and logout")
public class AuthController {
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsServiceImpl userDetailsService;
    private final UserServiceImpl userService;
    private final ArtistServiceImpl artistService;
    private final RefreshTokenServiceImpl refreshTokenService;
    private final IPasswordResetTokenService passwordResetTokenService;

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticates a user with username and password. Returns JWT access token and refresh token.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Login successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenResponse.class))), @ApiResponse(responseCode = "401", description = "Invalid username or password", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))), @ApiResponse(responseCode = "500", description = "Internal server error during authentication", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class)))})
    @Transactional
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password()));

        UserDetails user = (UserDetails) authentication.getPrincipal();
        Integer id = null;
        String img = null;
        String source = "";
        String role = user.getAuthorities().iterator().next().getAuthority();

        if (user instanceof Artist) {
            Artist artist = (Artist) user;
            id = artist.getId();
            img = artist.getImg();
            source = "artist";
        } else if (user instanceof vn.edu.fpt.sba.music4.entities.User) {
            vn.edu.fpt.sba.music4.entities.User u = (vn.edu.fpt.sba.music4.entities.User) user;
            id = u.getId();
            img = u.getImg();
            source = "user";
        }

        String accessToken = jwtUtil.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername(), source);

        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken.getToken(), id, user.getUsername(), img, role));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Access Token", description = "Generates a new access token and refresh token using a valid refresh token. Implements token rotation for security.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Token refresh successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenResponse.class))), @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))), @ApiResponse(responseCode = "500", description = "Internal server error during token refresh", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class)))})
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest req) {
        RefreshToken rt = refreshTokenService.verifyRefreshToken(req.refreshToken());
        UserDetails user = userDetailsService.loadUserByUsername(rt.getUsername());

        Integer id = null;
        String img = null;
        String role = user.getAuthorities().iterator().next().getAuthority();

        if (user instanceof Artist) {
            Artist artist = (Artist) user;
            id = artist.getId();
            img = artist.getImg();
        } else if (user instanceof vn.edu.fpt.sba.music4.entities.User) {
            vn.edu.fpt.sba.music4.entities.User u = (vn.edu.fpt.sba.music4.entities.User) user;
            id = u.getId();
            img = u.getImg();
        }

        String newAccess = jwtUtil.generateToken(user);
        RefreshToken newRt = refreshTokenService.rotate(rt);

        return ResponseEntity.ok(new TokenResponse(newAccess, newRt.getToken(), id, user.getUsername(), img, role));
    }

    @PostMapping("/logout")
    @Operation(summary = "User Logout", description = "Revokes the refresh token to invalidate user session. The user must login again to obtain new tokens.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Logout successful"), @ApiResponse(responseCode = "401", description = "Invalid refresh token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))), @ApiResponse(responseCode = "500", description = "Internal server error during logout", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class)))})
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest req) {
        refreshTokenService.revokeByToken(req.refreshToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "User Registration", description = "Register a new user (artist or listener). Different registration paths are supported based on the 'role' parameter.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "User registered successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))), @ApiResponse(responseCode = "400", description = "Bad request - Invalid role parameter or missing required fields", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))), @ApiResponse(responseCode = "409", description = "Conflict - Username or email already exists", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))), @ApiResponse(responseCode = "500", description = "Internal server error during registration", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class)))})
    public ResponseEntity<AuthResponse> register(@Parameter(name = "role", description = "User role for registration: 'artist' or 'listener'", required = true, example = "artist") @RequestParam("role") String role, @ModelAttribute RegistrationRequest registrationRequest) {
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
                try {
                    userService.save(registrationRequest);
                    return ResponseEntity.ok(new AuthResponse("User registered successfully"));
                } catch (IllegalArgumentException ex) {
                    return ResponseEntity.badRequest().body(new AuthResponse(ex.getMessage()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthResponse("An error occurred while registering user"));
                }
            }
            default -> {
                return ResponseEntity.badRequest().body(new AuthResponse("Invalid role specified. Must be 'artist' or 'listener'."));
            }
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetTokenService.initiatePasswordReset(request.email());
        return ResponseEntity.ok(new AuthResponse("Reset token is sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetTokenService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(new AuthResponse("Password has been reset."));
    }

    @GetMapping("/test")
    @Operation(summary = "Health Check", description = "Test endpoint to verify the auth service is running")
    @ApiResponse(responseCode = "200", description = "Service is healthy", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class)))
    public ResponseEntity<AuthResponse> test() {
        return ResponseEntity.ok(new AuthResponse("test"));
    }
}
