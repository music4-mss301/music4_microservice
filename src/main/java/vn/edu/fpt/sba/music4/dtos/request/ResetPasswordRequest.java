package vn.edu.fpt.sba.music4.dtos.request;

public record ResetPasswordRequest(String token, String newPassword) {
}
