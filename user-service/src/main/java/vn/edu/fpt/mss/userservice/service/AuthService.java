package vn.edu.fpt.mss.userservice.service;

import vn.edu.fpt.mss.userservice.dto.request.*;
import vn.edu.fpt.mss.userservice.dto.response.MessageResponse;
import vn.edu.fpt.mss.userservice.dto.response.TokenResponse;

public interface AuthService {
    MessageResponse register(RegisterRequest request, String role);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(RefreshRequest request);

    void logout(RefreshRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(Integer userId, ChangePasswordRequest request);
}
