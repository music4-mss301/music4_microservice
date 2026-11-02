package vn.edu.fpt.mss.engagementservice.dto.request;

import java.util.List;

public record ArtistEngagementQuery(Integer artistId, List<Integer> trackIds) {
}
