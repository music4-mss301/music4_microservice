package vn.edu.fpt.mss.engagementservice.dto.response;

import java.util.List;

public record PlaylistDetailResponse(PlaylistResponse playlist, List<PlaylistTrackItem> tracks) {
}
