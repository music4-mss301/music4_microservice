package vn.edu.fpt.sba.music4.dtos.response;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        Integer id,          // Thêm ID
        String username,     // Thêm Username
        String name,
        String img,          // Thêm link ảnh đại diện
        String role          // Thêm Role
) {}