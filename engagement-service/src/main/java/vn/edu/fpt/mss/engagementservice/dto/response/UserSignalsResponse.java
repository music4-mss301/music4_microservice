package vn.edu.fpt.mss.engagementservice.dto.response;

import java.util.List;

/** Tín hiệu cho gợi ý: favorite + rating >= 4 là thích, rating <= 2 là loại trừ. */
public record UserSignalsResponse(Integer userId, List<Integer> favoriteTrackIds, List<Integer> likedTrackIds,
                                  List<Integer> dislikedTrackIds) {
}
