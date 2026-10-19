package vn.edu.fpt.mss.contentservice.dto.event;

import java.time.LocalDate;

public record AlbumEventPayload(Integer albumId, String name, String img, LocalDate uploadDate, int trackTotal,
                                Integer artistId, String artistName) {
}
