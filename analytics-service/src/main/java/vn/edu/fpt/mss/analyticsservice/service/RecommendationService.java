package vn.edu.fpt.mss.analyticsservice.service;

import vn.edu.fpt.mss.analyticsservice.dto.response.RecommendationResponse;

import java.util.List;

public interface RecommendationService {
    List<RecommendationResponse> recommend(int size);
}
