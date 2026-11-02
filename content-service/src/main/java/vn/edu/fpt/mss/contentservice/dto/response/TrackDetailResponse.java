package vn.edu.fpt.mss.contentservice.dto.response;

import java.time.LocalDate;
import java.util.List;

public record TrackDetailResponse(Integer id, String name, String img, Integer duration, String filePath,
                                  String previewPath, LocalDate uploadDate, long viewCount, AlbumResponse album,
                                  List<TrackArtistInfo> artists, List<CategoryResponse> categories) {
}
