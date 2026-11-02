package vn.edu.fpt.mss.contentservice.dto.response;

import java.util.List;

public record ArtistDetailResponse(ArtistResponse artist, List<AlbumResponse> albums, List<TrackResponse> tracks) {
}
