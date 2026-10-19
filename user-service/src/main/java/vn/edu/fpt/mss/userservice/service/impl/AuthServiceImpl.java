package vn.edu.fpt.mss.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.mss.userservice.client.ContentClient;
import vn.edu.fpt.mss.userservice.dto.client.ArtistProfileResponse;
import vn.edu.fpt.mss.userservice.dto.client.CreateArtistProfileRequest;
import vn.edu.fpt.mss.userservice.dto.request.*;
import vn.edu.fpt.mss.userservice.dto.response.MessageResponse;
import vn.edu.fpt.mss.userservice.dto.response.TokenResponse;
import vn.edu.fpt.mss.userservice.entity.PasswordResetToken;
import vn.edu.fpt.mss.userservice.entity.RefreshToken;
import vn.edu.fpt.mss.userservice.entity.User;
import vn.edu.fpt.mss.userservice.enums.Role;
import vn.edu.fpt.mss.userservice.exception.*;
import vn.edu.fpt.mss.userservice.repository.PasswordResetTokenRepository;
import vn.edu.fpt.mss.userservice.repository.UserRepository;
import vn.edu.fpt.mss.userservice.service.AuthService;
import vn.edu.fpt.mss.userservice.service.EmailService;
import vn.edu.fpt.mss.userservice.service.JwtService;
import vn.edu.fpt.mss.userservice.service.RefreshTokenService;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int RESET_TOKEN_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ContentClient contentClient;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    /**
     * Rule: username/email unique; chỉ được tự đăng ký LISTENER hoặc ARTIST (không tự đăng ký ADMIN).
     * ARTIST: tạo hồ sơ bên content-service đồng bộ; content lỗi => rollback, không có user "mồ côi".
     */
    @Override
    @Transactional
    public MessageResponse register(RegisterRequest req, String roleParam) {
        Role role = parseSelfRegisterRole(roleParam);
        if (userRepository.existsByUserNameIgnoreCase(req.username())) {
            throw new BusinessException("Username đã tồn tại");
        }
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new BusinessException("Email đã được sử dụng");
        }
        User user = User.builder()
                .name(req.name().trim())
                .email(req.email().trim().toLowerCase())
                .userName(req.username().trim())
                .password(passwordEncoder.encode(req.password()))
                .role(role.name())
                .gender(req.gender())
                .build();
        user = userRepository.saveAndFlush(user);

        if (role == Role.ARTIST) {
            ArtistProfileResponse profile = contentClient.createArtistProfile(
                    new CreateArtistProfileRequest(user.getId(), user.getName(), user.getGender()));
            user.setArtistId(profile.id());
        }
        return new MessageResponse(role == Role.ARTIST ? "Đăng ký nghệ sĩ thành công" : "Đăng ký thành công");
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest req) {
        String login = req.username().trim();
        User user = (login.contains("@") ? userRepository.findByEmailIgnoreCase(login)
                : userRepository.findByUserNameIgnoreCase(login))
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new UnauthorizedException("Sai tên đăng nhập hoặc mật khẩu"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new UnauthorizedException("Sai tên đăng nhập hoặc mật khẩu");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new ForbiddenException("Tài khoản đã bị khóa");
        }
        return issueTokens(user);
    }

    @Override
    @Transactional
    public TokenResponse refresh(RefreshRequest req) {
        RefreshToken old = refreshTokenService.verify(req.refreshToken());
        User user = userRepository.findByUserNameIgnoreCase(old.getUsername())
                .filter(u -> Boolean.TRUE.equals(u.getEnabled()) && !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new UnauthorizedException("Tài khoản không còn hoạt động"));
        RefreshToken rotated = refreshTokenService.rotate(old);
        return toTokenResponse(user, rotated.getToken());
    }

    @Override
    public void logout(RefreshRequest req) {
        refreshTokenService.revoke(req.refreshToken());
    }

    /** Luôn trả 200 dù email không tồn tại để không lộ email nào đã đăng ký. */
    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        userRepository.findByEmailIgnoreCase(req.email())
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .ifPresent(user -> {
                    resetTokenRepository.deleteByEmail(user.getEmail());
                    PasswordResetToken token = resetTokenRepository.save(PasswordResetToken.builder()
                            .token(UUID.randomUUID().toString())
                            .email(user.getEmail())
                            .accountType(user.getRole())
                            .expiryDate(LocalDateTime.now().plusMinutes(RESET_TOKEN_MINUTES))
                            .used(false)
                            .build());
                    emailService.sendPasswordReset(user.getEmail(),
                            frontendUrl + "/reset-password?token=" + token.getToken());
                });
    }

    /** Rule: token dùng một lần, có hạn 30 phút; đổi mật khẩu xong thu hồi mọi refresh token. */
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        PasswordResetToken token = resetTokenRepository.findByToken(req.token())
                .orElseThrow(() -> new BadRequestException("Token đặt lại mật khẩu không hợp lệ"));
        if (token.isUsed() || token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token đã được dùng hoặc đã hết hạn");
        }
        User user = userRepository.findByEmailIgnoreCase(token.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        token.setUsed(true);
        refreshTokenService.revokeAllOf(user.getUserName());
    }

    @Override
    @Transactional
    public void changePassword(Integer userId, ChangePasswordRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu hiện tại không đúng");
        }
        if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu mới phải khác mật khẩu cũ");
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        refreshTokenService.revokeAllOf(user.getUserName());
    }

    private TokenResponse issueTokens(User user) {
        RefreshToken refresh = refreshTokenService.create(user.getUserName(),
                "ARTIST".equals(user.getRole()) ? "artist" : "user");
        return toTokenResponse(user, refresh.getToken());
    }

    private TokenResponse toTokenResponse(User user, String refreshToken) {
        return new TokenResponse(jwtService.generateAccessToken(user), refreshToken, "Bearer",
                jwtService.accessExpirationSeconds(), user.getId(), user.getUserName(), user.getName(),
                user.getImg(), user.getRole(), user.getArtistId());
    }

    private Role parseSelfRegisterRole(String roleParam) {
        Role role;
        try {
            role = Role.from(roleParam);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Role không hợp lệ. Chỉ chấp nhận 'listener' hoặc 'artist'");
        }
        if (role == Role.ADMIN) {
            throw new ForbiddenException("Không thể tự đăng ký tài khoản ADMIN");
        }
        return role;
    }
}
