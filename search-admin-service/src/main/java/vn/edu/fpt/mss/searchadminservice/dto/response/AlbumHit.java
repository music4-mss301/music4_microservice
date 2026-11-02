package vn.edu.fpt.mss.searchadminservice.dto.response;

public record AlbumHit(Integer albumId, String name, String img, Integer artistId, String artistName, int trackTotal) {
}
