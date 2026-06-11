package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.dtos.request.RatingRequest;

public interface IRatingService {
    void saveRating(RatingRequest request);
}