package vn.edu.fpt.sba.music4.dtos.response;

public record UserResponseDTO(
        Integer id,
        String name,
        String img,
        String email,
        Boolean gender,
        String role,
        String username
) {
}

