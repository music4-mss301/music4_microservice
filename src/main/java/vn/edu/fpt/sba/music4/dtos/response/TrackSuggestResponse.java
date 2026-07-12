package vn.edu.fpt.sba.music4.dtos.response;

import java.util.List;

public record TrackSuggestResponse(
        Integer id,
        String name,
        String img,
        Integer duration,
        String previewPath,
        Integer viewCount,
        Double matchScore,
        List<TrackArtistInfo> artists
) {
}