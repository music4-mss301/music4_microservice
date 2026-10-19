package vn.edu.fpt.mss.searchadminservice.dto.response;

import java.util.List;

public record TrackHit(Integer trackId, String name, String img, Integer duration, String previewPath,
                       long viewCount, Integer albumId, String albumName, List<String> artistNames,
                       boolean pinned, Double score) {
}
