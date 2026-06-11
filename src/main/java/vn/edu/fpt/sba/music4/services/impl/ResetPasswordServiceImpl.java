package vn.edu.fpt.sba.music4.services.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.fpt.sba.music4.entities.PasswordResetToken;
import vn.edu.fpt.sba.music4.entities.User;
import vn.edu.fpt.sba.music4.repositories.PasswordResetTokenRepository;
import vn.edu.fpt.sba.music4.repositories.UserRepository;
import vn.edu.fpt.sba.music4.services.IPasswordResetTokenService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResetPasswordServiceImpl implements IPasswordResetTokenService {
    private final PasswordResetTokenRepository passwordResetTokenRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final int TOKEN_EXPIRY_MINUTES = 15;


    @Override
    @Transactional
    public void initiatePasswordReset(String email) {
        userRepo.findByEmail(email).ifPresent(user -> {
            passwordResetTokenRepo.deleteByUserAndUsedFalse(user);

            String token = generateSecureToken();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setUser(user);
            resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES));
            passwordResetTokenRepo.save(resetToken);

            System.out.println("Sent email to: " + email);
            emailService.sendResetEmail(email, token);
        });
        // Luôn trả về response giống nhau dù email có tồn tại hay không
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        if (!resetToken.isValid()) {
            throw new RuntimeException("Token is invalid or expired");
        }

        User user =  resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepo.save(resetToken);

        invalidateAllUserSessions(user);
    }

    @Override
    public String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public void invalidateAllUserSessions(User user) {

    }
}
