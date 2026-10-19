package vn.edu.fpt.mss.engagementservice.dto.event;

/** Dùng chung cho content.artist.deleted (artistId) và user.account.deleted (userId). */
public record IdPayload(Integer artistId, Integer userId) {
}
