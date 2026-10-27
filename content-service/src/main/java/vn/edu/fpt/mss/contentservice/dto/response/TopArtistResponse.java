package vn.edu.fpt.mss.contentservice.dto.response;

/** Dùng cho dashboard admin: followers + favorites lấy từ engagement-service. */
public record TopArtistResponse(Integer id, String name, String img, int trackTotal, long followers,
                                long favorites) {
}
