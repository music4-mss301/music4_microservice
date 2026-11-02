package vn.edu.fpt.mss.searchadminservice.dto.response;

/** source = elasticsearch | database (fallback khi ES lỗi). */
public record SearchResponse(String query, HitPage<TrackHit> tracks, HitPage<AlbumHit> albums,
                             HitPage<ArtistHit> artists, String source) {
}
