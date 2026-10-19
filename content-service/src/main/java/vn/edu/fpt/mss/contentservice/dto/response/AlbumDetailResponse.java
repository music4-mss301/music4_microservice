package vn.edu.fpt.mss.contentservice.dto.response;

import java.util.List;

public record AlbumDetailResponse(AlbumResponse album, List<TrackResponse> tracks) {
}
