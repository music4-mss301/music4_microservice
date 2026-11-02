package vn.edu.fpt.mss.contentservice.dto.response;

import java.util.List;

public record CategoryDetailResponse(Integer id, String name, List<TrackResponse> tracks) {
}
