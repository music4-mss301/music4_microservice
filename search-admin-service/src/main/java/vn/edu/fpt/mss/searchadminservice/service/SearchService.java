package vn.edu.fpt.mss.searchadminservice.service;

import vn.edu.fpt.mss.searchadminservice.dto.request.RecommendQuery;
import vn.edu.fpt.mss.searchadminservice.dto.response.SearchResponse;
import vn.edu.fpt.mss.searchadminservice.dto.response.SuggestionResponse;

import java.util.List;

public interface SearchService {
    SearchResponse search(String q, String type, int page, int size);

    List<String> suggest(String q);

    List<SuggestionResponse> recommend(RecommendQuery query);
}
