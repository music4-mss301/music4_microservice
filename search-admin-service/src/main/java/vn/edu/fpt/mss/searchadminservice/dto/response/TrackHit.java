package vn.edu.fpt.mss.searchadminservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import vn.edu.fpt.mss.searchadminservice.dto.client.TrackDoc;

import java.util.List;

public record TrackHit(Integer trackId, String name, String img, Integer duration, String previewPath,
                       long viewCount, Integer albumId, String albumName, List<String> artistNames,
                       List<TrackDoc.Ref> artists, boolean pinned, Double score) {

    public TrackHit(Integer trackId, String name, String img, Integer duration, String previewPath,
                    long viewCount, Integer albumId, String albumName, List<String> artistNames,
                    boolean pinned, Double score) {
        this(trackId, name, img, duration, previewPath, viewCount, albumId, albumName, artistNames, List.of(), pinned, score);
    }

    @JsonProperty("id")
    public Integer id() {
        return trackId;
    }
}
