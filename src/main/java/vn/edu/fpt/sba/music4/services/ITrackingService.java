package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.dtos.request.SyncTimeRequest;

public interface ITrackingService {

    public void recordPlayHistory(Integer trackId, Integer userId);

    void syncPlaybackPosition(SyncTimeRequest request);

}
