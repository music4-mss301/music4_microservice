package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.entities.RefreshToken;

public interface IRefreshTokenService {
    RefreshToken createRefreshToken(String username, String source);
    RefreshToken verifyRefreshToken(String token);
    RefreshToken rotate(RefreshToken old);
    void revokeByToken(String token);
}
