package vn.edu.fpt.mss.userservice.dto.response;

/** Giữ đúng các field FE đang đọc: accessToken, refreshToken, id, username, img, role. */
public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn,
                            Integer id, String username, String name, String img, String role, Integer artistId) {
}
