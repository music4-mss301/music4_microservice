package vn.edu.fpt.mss.searchadminservice.dto.response;

public record ArtistHit(Integer artistId, String name, String img, int trackTotal, int albumTotal) {
}
