package vn.edu.fpt.sba.music4.dtos.response;

public record TopTrackResponse(
        Long id,
        String name,
        Integer viewCount,
        Integer favoriteCount
) {
}
