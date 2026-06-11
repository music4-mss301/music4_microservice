package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.dtos.response.SearchResultResponseDTO;

public interface ISearchService {
        SearchResultResponseDTO search(String keyword, String type, int page, int size);
    }
