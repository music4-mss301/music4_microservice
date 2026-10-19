package vn.edu.fpt.mss.searchadminservice.service;

import vn.edu.fpt.mss.searchadminservice.dto.request.CurationRequest;
import vn.edu.fpt.mss.searchadminservice.dto.response.CurationResponse;

import java.util.List;
import java.util.Optional;

public interface CurationService {
    List<CurationResponse> getAll();

    CurationResponse get(String id);

    CurationResponse create(CurationRequest request);

    CurationResponse update(String id, CurationRequest request);

    void delete(String id);

    Optional<CurationResponse> findByKeyword(String keyword);
}
