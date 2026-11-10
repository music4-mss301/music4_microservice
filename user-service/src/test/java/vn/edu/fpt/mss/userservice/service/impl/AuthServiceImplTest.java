package vn.edu.fpt.mss.userservice.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.fpt.mss.userservice.client.ContentClient;
import vn.edu.fpt.mss.userservice.dto.request.LoginRequest;
import vn.edu.fpt.mss.userservice.dto.request.RegisterRequest;
import vn.edu.fpt.mss.userservice.entity.User;
import vn.edu.fpt.mss.userservice.exception.BusinessException;
import vn.edu.fpt.mss.userservice.exception.ForbiddenException;
import vn.edu.fpt.mss.userservice.exception.UnauthorizedException;
import vn.edu.fpt.mss.userservice.repository.PasswordResetTokenRepository;
import vn.edu.fpt.mss.userservice.repository.UserRepository;
import vn.edu.fpt.mss.userservice.service.EmailService;
import vn.edu.fpt.mss.userservice.service.JwtService;
import vn.edu.fpt.mss.userservice.service.RefreshTokenService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Business rule đăng ký / đăng nhập (không cần DB, JWT hay RabbitMQ: tất cả được giả lập bằng Mockito). */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository resetTokenRepository;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private ContentClient contentClient;
    @InjectMocks
    private AuthServiceImpl service;

    private RegisterRequest register(String email) {
        return new RegisterRequest("New User", email, "newuser", "Passw0rdOK", true);
    }

    private User user(boolean enabled) {
        return User.builder().id(5).name("Demo").email("demo@music4.local").userName("demo")
                .password("hash").role("LISTENER").enabled(enabled).build();
    }

    // ---------------------------------------------------------------- Rule: đăng ký

    @Test
    void register_withEmailAlreadyUsed_isConflict() {
        when(userRepository.existsByUserNameIgnoreCase("newuser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("used@music4.local")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.register(register("used@music4.local"), "listener"));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void register_withUsernameAlreadyUsed_isConflict() {
        when(userRepository.existsByUserNameIgnoreCase("newuser")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.register(register("new@music4.local"), "listener"));
    }

    @Test
    void register_asAdmin_isForbidden() {
        assertThrows(ForbiddenException.class, () -> service.register(register("new@music4.local"), "admin"));
        verify(userRepository, never()).saveAndFlush(any());
    }

    // ---------------------------------------------------------------- Rule: đăng nhập

    @Test
    void login_withWrongPassword_isUnauthorized() {
        when(userRepository.findByUserNameIgnoreCase("demo")).thenReturn(Optional.of(user(true)));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> service.login(new LoginRequest("demo", "wrong")));
    }

    @Test
    void login_withLockedAccount_isForbidden() {
        when(userRepository.findByUserNameIgnoreCase("demo")).thenReturn(Optional.of(user(false)));
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);

        assertThrows(ForbiddenException.class, () -> service.login(new LoginRequest("demo", "secret123")));
    }
}
