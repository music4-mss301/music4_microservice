package vn.edu.fpt.mss.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.mss.userservice.config.JwtProperties;
import vn.edu.fpt.mss.userservice.entity.RefreshToken;
import vn.edu.fpt.mss.userservice.exception.UnauthorizedException;
import vn.edu.fpt.mss.userservice.repository.RefreshTokenRepository;
import vn.edu.fpt.mss.userservice.service.RefreshTokenService;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public RefreshToken create(String username, String source) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .username(username)
                .source(source)
                .expiryDate(LocalDateTime.now().plusDays(jwtProperties.refreshExpirationDays()))
                .revoked(false)
                .build();
        return repository.save(token);
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken verify(String token) {
        RefreshToken rt = repository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Refresh token không tồn tại"));
        if (rt.isRevoked()) {
            throw new UnauthorizedException("Refresh token đã bị thu hồi");
        }
        if (rt.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token đã hết hạn, vui lòng đăng nhập lại");
        }
        return rt;
    }

    /** Rotation: token cũ bị xóa, phát token mới. Dùng lại token cũ sẽ bị từ chối. */
    @Override
    @Transactional
    public RefreshToken rotate(RefreshToken old) {
        repository.delete(old);
        return create(old.getUsername(), old.getSource());
    }

    @Override
    @Transactional
    public void revoke(String token) {
        repository.findByToken(token).ifPresent(rt -> rt.setRevoked(true));
    }

    @Override
    @Transactional
    public void revokeAllOf(String username) {
        repository.deleteByUsername(username);
    }
}
