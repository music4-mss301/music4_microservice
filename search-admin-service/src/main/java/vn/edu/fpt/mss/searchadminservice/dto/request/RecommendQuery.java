package vn.edu.fpt.mss.searchadminservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

public record RecommendQuery(List<Integer> likedTrackIds, List<Integer> dislikedTrackIds,
                             List<Integer> excludeTrackIds, @Min(1) @Max(50) int size) {
}
