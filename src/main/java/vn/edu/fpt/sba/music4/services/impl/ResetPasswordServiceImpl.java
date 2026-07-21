package vn.edu.fpt.sba.music4.services.impl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.fpt.sba.music4.entities.Artist;
import vn.edu.fpt.sba.music4.entities.PasswordResetToken;
import vn.edu.fpt.sba.music4.entities.User;
import vn.edu.fpt.sba.music4.repositories.ArtistRepository;
import vn.edu.fpt.sba.music4.repositories.PasswordResetTokenRepository;
import vn.edu.fpt.sba.music4.repositories.UserRepository;
import vn.edu.fpt.sba.music4.services.IPasswordResetTokenService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResetPasswordServiceImpl implements IPasswordResetTokenService {
    private final PasswordResetTokenRepository passwordResetTokenRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final int TOKEN_EXPIRY_MINUTES = 15;
    private final ArtistRepository artistRepository;


    @Override
    @Transactional
    public void initiatePasswordReset(String email) {
        Optional<User> userOpt = userRepo.findByEmail(email);

        if (userOpt.isPresent()) {
            createAndSendToken(email, PasswordResetToken.AccountType.USER);
            return;
        }

        Optional<Artist> artistOpt = artistRepository.findByEmail(email);
        if (artistOpt.isPresent()) {
            createAndSendToken(email, PasswordResetToken.AccountType.ARTIST);
            return;
        }

        System.out.println("Initiated reset flow for: " + email);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepo.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Đường dẫn khôi phục không tồn tại hoặc không hợp lệ."));

        if (!resetToken.isValid()) {
            throw new IllegalStateException("Đường dẫn khôi phục đã hết hạn hoặc đã được sử dụng.");
        }

        String email = resetToken.getEmail();
        String encodedPassword = passwordEncoder.encode(newPassword);

        if (resetToken.getAccountType() == PasswordResetToken.AccountType.ARTIST) {
            Artist artist = artistRepository.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản Nghệ sĩ liên kết với email này."));
            artist.setPassword(encodedPassword);
            artistRepository.save(artist);
        } else {
            User user = userRepo.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản Người dùng liên kết với email này."));
            user.setPassword(encodedPassword);
            userRepo.save(user);
        }

        resetToken.setUsed(true);
        passwordResetTokenRepo.save(resetToken);

        passwordResetTokenRepo.deleteByEmailAndUsedFalse(email);
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

    private void createAndSendToken(String email, PasswordResetToken.AccountType accountType) {
        passwordResetTokenRepo.deleteByEmailAndUsedFalse(email);

        String token = generateSecureToken();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setEmail(email);
        resetToken.setAccountType(accountType);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES));

        passwordResetTokenRepo.save(resetToken);

        System.out.println("Sent email to " + accountType + ": " + email);
        emailService.sendResetEmail(email, token);
    }
}
