package vn.edu.fpt.mss.searchadminservice.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import vn.edu.fpt.mss.searchadminservice.dto.client.TrackSummary;
import vn.edu.fpt.mss.searchadminservice.dto.response.*;

import java.util.ArrayList;
import java.util.List;

/** Chuyển document ES (JsonNode) sang DTO trả cho FE. */
public final class SearchMapper {

    private SearchMapper() {
    }

    public static TrackHit toTrackHit(JsonNode hit, boolean pinned) {
        JsonNode s = hit.path("_source");
        List<String> artists = new ArrayList<>();
        s.path("artistNames").forEach(a -> artists.add(a.asText()));
        return new TrackHit(intOrNull(s, "trackId"), s.path("name").asText(null), s.path("img").asText(null),
                intOrNull(s, "duration"), s.path("previewPath").asText(null), s.path("viewCount").asLong(0),
                intOrNull(s, "albumId"), s.path("albumName").asText(null), artists, pinned,
                hit.hasNonNull("_score") ? hit.get("_score").asDouble() : null);
    }

    public static AlbumHit toAlbumHit(JsonNode hit) {
        JsonNode s = hit.path("_source");
        return new AlbumHit(intOrNull(s, "albumId"), s.path("name").asText(null), s.path("img").asText(null),
                intOrNull(s, "artistId"), s.path("artistName").asText(null), s.path("trackTotal").asInt(0));
    }

    public static ArtistHit toArtistHit(JsonNode hit) {
        JsonNode s = hit.path("_source");
        return new ArtistHit(intOrNull(s, "artistId"), s.path("name").asText(null), s.path("img").asText(null),
                s.path("trackTotal").asInt(0), s.path("albumTotal").asInt(0));
    }

    public static SuggestionResponse toSuggestion(JsonNode hit) {
        JsonNode s = hit.path("_source");
        return new SuggestionResponse(intOrNull(s, "trackId"), s.path("name").asText(null), s.path("img").asText(null),
                intOrNull(s, "duration"), s.path("previewPath").asText(null), s.path("viewCount").asLong(0),
                hit.hasNonNull("_score") ? hit.get("_score").asDouble() : null);
    }

    public static TrackHit fromDb(TrackSummary t) {
        return new TrackHit(t.id(), t.name(), t.img(), t.duration(), t.previewPath(), t.viewCount(), t.albumId(),
                t.albumName(), List.of(), false, null);
    }

    private static Integer intOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asInt() : null;
    }
}
