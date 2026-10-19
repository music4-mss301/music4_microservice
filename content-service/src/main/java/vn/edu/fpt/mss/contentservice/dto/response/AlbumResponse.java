package vn.edu.fpt.mss.contentservice.dto.response;

import java.time.LocalDate;

public record AlbumResponse(Integer id, String name, String img, LocalDate uploadDate, int trackTotal,
                            Integer artistId, String artistName) {
}
