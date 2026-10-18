package vn.edu.fpt.mss.analyticsservice.dto.client;

public record Suggestion(Integer trackId, String name, String img, Integer duration, String previewPath,
                         long viewCount, Double score) {
}
