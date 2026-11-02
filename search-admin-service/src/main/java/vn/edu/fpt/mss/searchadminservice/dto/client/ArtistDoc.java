package vn.edu.fpt.mss.searchadminservice.dto.client;

public record ArtistDoc(Integer artistId, Integer userId, String name, String img, int trackTotal, int albumTotal) {
}
