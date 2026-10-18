package vn.edu.fpt.mss.analyticsservice.service;

import vn.edu.fpt.mss.analyticsservice.dto.request.SyncTimeRequest;
import vn.edu.fpt.mss.analyticsservice.dto.response.HistoryResponse;
import vn.edu.fpt.mss.analyticsservice.dto.response.PageResponse;
import vn.edu.fpt.mss.analyticsservice.dto.response.PlayResponse;

public interface TrackingService {
    PlayResponse recordPlay(Integer trackId);

    void syncPosition(SyncTimeRequest request);

    PageResponse<HistoryResponse> myHistory(int page, int size);

    void deleteEntry(Integer id);

    int clearMine();

    int cleanupOlderThan(int days);

    void anonymizeUser(Integer userId);
}
