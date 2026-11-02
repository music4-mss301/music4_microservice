package vn.edu.fpt.mss.userservice.dto.client;

public record CreateArtistProfileRequest(Integer userId, String name, Boolean gender) {
}
