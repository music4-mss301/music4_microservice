package vn.edu.fpt.sba.music4.dtos.response;

public record TrackSuggestResponse(
        Integer trackId,
        String trackName,
        String img,
        String duration,
        String previewPath,
        Integer viewCount,
        Double matchScore
) {
}