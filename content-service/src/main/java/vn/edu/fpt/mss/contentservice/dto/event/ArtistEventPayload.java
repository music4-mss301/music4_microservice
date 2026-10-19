package vn.edu.fpt.mss.contentservice.dto.event;

public record ArtistEventPayload(Integer artistId, Integer userId, String name, String img, int trackTotal,
                                 int albumTotal) {
}
