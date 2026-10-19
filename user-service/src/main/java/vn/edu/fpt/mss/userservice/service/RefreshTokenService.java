package vn.edu.fpt.mss.userservice.service;

import vn.edu.fpt.mss.userservice.entity.RefreshToken;

public interface RefreshTokenService {
    RefreshToken create(String username, String source);

    RefreshToken verify(String token);

    RefreshToken rotate(RefreshToken old);

    void revoke(String token);

    void revokeAllOf(String username);
}
