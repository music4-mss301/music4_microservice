package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.dtos.response.TrackSuggestResponse;

import java.util.List;

public interface IRecommendationService {

    List<TrackSuggestResponse> getRecommendations(Integer userId);

    void syncTracksToElasticsearch();
}
