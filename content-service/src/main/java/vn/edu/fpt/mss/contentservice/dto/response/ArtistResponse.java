package vn.edu.fpt.mss.contentservice.dto.response;

public record ArtistResponse(Integer id, Integer userId, String name, String img, String cover, Boolean gender,
                             int trackTotal, int albumTotal) {
}
