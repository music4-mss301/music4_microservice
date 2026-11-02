package vn.edu.fpt.mss.searchadminservice.dto.client;

import java.time.LocalDate;

public record AlbumDoc(Integer albumId, String name, String img, LocalDate uploadDate, int trackTotal,
                       Integer artistId, String artistName) {
}
