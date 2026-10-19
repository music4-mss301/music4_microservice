package vn.edu.fpt.mss.contentservice.service;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.mss.contentservice.dto.request.AlbumBulkRequest;
import vn.edu.fpt.mss.contentservice.dto.request.TrackUpdateRequest;
import vn.edu.fpt.mss.contentservice.dto.request.TrackUploadRequest;
import vn.edu.fpt.mss.contentservice.dto.response.*;
import vn.edu.fpt.mss.contentservice.dto.event.TrackEventPayload;

import java.util.List;

public interface TrackService {
    PageResponse<TrackResponse> getAll(int page, int size, String sort);

    TrackDetailResponse getDetail(Integer id);

    List<TrackResponse> getByAlbum(Integer albumId);

    List<TrackResponse> getByArtist(Integer artistId);

    List<TrackResponse> getTrending(int limit);

    TrackDetailResponse upload(MultipartFile audioFile, MultipartFile coverImage, TrackUploadRequest request);

    UploadTempResponse uploadTemp(MultipartFile file, String type);

    List<TrackResponse> saveBulk(AlbumBulkRequest request);

    TrackDetailResponse update(Integer id, TrackUpdateRequest request);

    void delete(Integer id);

    String resolveStreamKey(Integer id, boolean preview);

    List<TrackResponse> getByIds(List<Integer> ids);

    PageResponse<TrackEventPayload> exportForIndex(int page, int size);

    PageResponse<TrackResponse> search(String keyword, int page, int size);

    void incrementView(Integer trackId);

    ContentStatsResponse stats();
}
