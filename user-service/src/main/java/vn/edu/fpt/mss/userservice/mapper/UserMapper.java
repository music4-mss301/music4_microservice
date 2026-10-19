package vn.edu.fpt.mss.userservice.mapper;

import vn.edu.fpt.mss.userservice.dto.response.UserResponse;
import vn.edu.fpt.mss.userservice.dto.response.UserSummaryResponse;
import vn.edu.fpt.mss.userservice.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getUserName(), u.getEmail(), u.getImg(), u.getRole(),
                u.getArtistId(), u.getGender(), u.getEnabled(), u.getCreatedAt());
    }

    public static UserSummaryResponse toSummary(User u) {
        return new UserSummaryResponse(u.getId(), u.getName(), u.getUserName(), u.getImg(), u.getRole());
    }
}
