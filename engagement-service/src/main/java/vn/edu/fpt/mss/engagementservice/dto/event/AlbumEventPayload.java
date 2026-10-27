package vn.edu.fpt.mss.engagementservice.dto.event;

/** Chỉ đọc các field cần từ event content.album.created. */
public record AlbumEventPayload(Integer albumId, String name, Integer artistId, String artistName) {
}
