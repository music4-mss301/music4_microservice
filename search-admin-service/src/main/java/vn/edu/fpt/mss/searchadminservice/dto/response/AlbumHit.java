package vn.edu.fpt.mss.searchadminservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AlbumHit(Integer albumId, String name, String img, Integer artistId, String artistName, int trackTotal) {

    @JsonProperty("id")
    public Integer id() {
        return albumId;
    }
}
