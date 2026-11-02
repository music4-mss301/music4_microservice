package vn.edu.fpt.mss.searchadminservice.dto.response;

public record SuggestionResponse(Integer trackId, String name, String img, Integer duration, String previewPath,
                                 long viewCount, Double score) {
}
