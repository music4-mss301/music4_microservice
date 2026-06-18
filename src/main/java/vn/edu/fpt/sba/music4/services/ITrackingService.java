package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.dtos.request.SyncTimeRequest;
import vn.edu.fpt.sba.music4.dtos.response.TrackResponseDTO;

import java.util.List;

public interface ITrackingService {

    void recordPlayHistory(Integer trackId, Integer userId);

    void syncPlaybackPosition(SyncTimeRequest request);

    Page<TrackResponseDTO> getListeningHistory(Integer userId);
}
