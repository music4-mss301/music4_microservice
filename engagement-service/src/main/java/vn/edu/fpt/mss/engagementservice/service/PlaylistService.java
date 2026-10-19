package vn.edu.fpt.mss.engagementservice.service;

import vn.edu.fpt.mss.engagementservice.dto.request.PlaylistRequest;
import vn.edu.fpt.mss.engagementservice.dto.response.PlaylistDetailResponse;
import vn.edu.fpt.mss.engagementservice.dto.response.PlaylistResponse;

import java.util.List;

public interface PlaylistService {
    List<PlaylistResponse> getMine();

    List<PlaylistResponse> getByUser(Integer userId);

    PlaylistDetailResponse getDetail(Integer id);

    PlaylistResponse create(PlaylistRequest request);

    PlaylistResponse update(Integer id, PlaylistRequest request);

    PlaylistResponse updateImage(Integer id, String img);

    void delete(Integer id);

    PlaylistDetailResponse addTrack(Integer playlistId, Integer trackId);

    void removeTrack(Integer playlistId, Integer trackId);
}
