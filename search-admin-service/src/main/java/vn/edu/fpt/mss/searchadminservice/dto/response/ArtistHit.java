package vn.edu.fpt.mss.searchadminservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ArtistHit(Integer artistId, String name, String img, int trackTotal, int albumTotal) {

    @JsonProperty("id")
    public Integer id() {
        return artistId;
    }
}
