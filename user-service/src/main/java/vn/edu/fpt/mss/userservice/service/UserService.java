package vn.edu.fpt.mss.userservice.service;

import vn.edu.fpt.mss.userservice.dto.request.UpdateProfileRequest;
import vn.edu.fpt.mss.userservice.dto.response.PageResponse;
import vn.edu.fpt.mss.userservice.dto.response.UserResponse;
import vn.edu.fpt.mss.userservice.dto.response.UserStatsResponse;
import vn.edu.fpt.mss.userservice.dto.response.UserSummaryResponse;

import java.util.List;

public interface UserService {
    UserResponse getById(Integer id);

    PageResponse<UserResponse> search(String keyword, String role, int page, int size);

    UserResponse updateProfile(Integer id, UpdateProfileRequest request);

    UserResponse changeRole(Integer id, String role);

    UserResponse changeStatus(Integer id, boolean enabled);

    void delete(Integer id);

    List<UserSummaryResponse> getSummaries(List<Integer> ids);

    UserStatsResponse stats();
}
