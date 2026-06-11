package vn.edu.fpt.sba.music4.services;

import jakarta.transaction.Transactional;
import vn.edu.fpt.sba.music4.entities.User;

public interface IPasswordResetTokenService {
    void initiatePasswordReset(String email);
    void resetPassword(String token, String newPassword);
    String generateSecureToken();
    void invalidateAllUserSessions(User user);

}
