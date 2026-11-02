package vn.edu.fpt.mss.contentservice.dto.client;

import java.util.List;

public record ArtistEngagementQuery(Integer artistId, List<Integer> trackIds) {
}
