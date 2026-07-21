package vn.edu.fpt.sba.music4.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.sba.music4.entities.Artist;
import vn.edu.fpt.sba.music4.entities.User;
import vn.edu.fpt.sba.music4.entities.VerificationToken;
import vn.edu.fpt.sba.music4.repositories.ArtistRepository;
import vn.edu.fpt.sba.music4.repositories.UserRepository;
import vn.edu.fpt.sba.music4.repositories.VerificationTokenRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationService {
    private final VerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final EmailService emailService;

    public void createAndSendToken(String email, String role) {
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, email, role);
        tokenRepository.save(verificationToken);
        emailService.sendVerificationEmail(email, token);
    }

    @Transactional
    public boolean verifyAccount(String token) {
        Optional<VerificationToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Token không hợp lệ!");
        }

        VerificationToken vt = tokenOpt.get();
        if (vt.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(vt);
            throw new IllegalArgumentException("Token đã hết hạn!");
        }

        if ("ARTIST".equalsIgnoreCase(vt.getRole())) {
            Artist artist = artistRepository.findByEmail(vt.getEmail()).orElseThrow(() -> new RuntimeException("Artist not found"));

            artist.setEnabled(true);
            artistRepository.save(artist);
        } else {
            User user = userRepository.findByEmail(vt.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setEnabled(true);
            userRepository.save(user);
        }

        tokenRepository.delete(vt);
        return true;
    }
}