package vn.edu.fpt.mss.searchadminservice.dto.client;

import java.time.LocalDate;
import java.util.List;

/** Payload content.track.* (cũng là kết quả /internal/tracks/export khi reindex). */
public record TrackDoc(Integer trackId, String name, String img, Integer duration, String filePath, String previewPath,
                       LocalDate uploadDate, long viewCount, Integer albumId, String albumName,
                       List<Ref> artists, List<Ref> categories) {
    public record Ref(Integer id, String name) {
    }
}
