package vn.edu.fpt.mss.userservice.dto.response;

/** Bản rút gọn trả cho service khác (tên/avatar cho comment...). */
public record UserSummaryResponse(Integer id, String name, String username, String img, String role) {
}
