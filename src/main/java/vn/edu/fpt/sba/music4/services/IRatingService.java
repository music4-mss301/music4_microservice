package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.dtos.request.RatingRequest;
import vn.edu.fpt.sba.music4.dtos.response.RatingResponseDTO;

public interface IRatingService {
    void saveRating(RatingRequest request);
    int getRatingByUserAndTrack(Integer userId, Integer trackId);
    double getAverageRating(Integer trackId);
    long countRatings(Integer trackId);
    RatingResponseDTO getRatingStats(Integer userId, Integer trackId);
}