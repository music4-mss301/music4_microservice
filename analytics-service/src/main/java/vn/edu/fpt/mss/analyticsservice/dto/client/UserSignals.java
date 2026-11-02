package vn.edu.fpt.mss.analyticsservice.dto.client;

import java.util.List;

public record UserSignals(Integer userId, List<Integer> favoriteTrackIds, List<Integer> likedTrackIds,
                          List<Integer> dislikedTrackIds) {
    public static UserSignals empty(Integer userId) {
        return new UserSignals(userId, List.of(), List.of(), List.of());
    }
}
