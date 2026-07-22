package vn.edu.fpt.sba.music4.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.fpt.sba.music4.dtos.request.SyncTimeRequest;
import vn.edu.fpt.sba.music4.dtos.request.TrackPlayRequest;
import vn.edu.fpt.sba.music4.dtos.response.TrackResponseDTO;

import java.util.List;

public interface ITrackingService {

    void recordPlayHistory(Integer trackId, Integer userId, String currentUsername, String currentRole);

    void syncPlaybackPosition(SyncTimeRequest request);

    Page<TrackResponseDTO> getListeningHistory(Integer userId, Pageable pageable);

    void cleanupHistory();
}
