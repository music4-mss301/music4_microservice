package vn.edu.fpt.mss.contentservice.dto.event;

import java.time.LocalDate;
import java.util.List;

/** Payload denormalize để consumer (search, engagement) không phải gọi ngược về content-service. */
public record TrackEventPayload(Integer trackId, String name, String img, Integer duration, String filePath,
                                String previewPath, LocalDate uploadDate, long viewCount, Integer albumId,
                                String albumName, List<RefPayload> artists, List<RefPayload> categories) {
}
