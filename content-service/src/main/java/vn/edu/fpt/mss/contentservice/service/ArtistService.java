package vn.edu.fpt.mss.contentservice.service;

import vn.edu.fpt.mss.contentservice.dto.request.ArtistUpdateRequest;
import vn.edu.fpt.mss.contentservice.dto.request.CreateArtistProfileRequest;
import vn.edu.fpt.mss.contentservice.dto.response.*;
import vn.edu.fpt.mss.contentservice.entity.Artist;

import java.util.List;

public interface ArtistService {
    List<ArtistResponse> getAll();

    List<ArtistResponse> getByIds(List<Integer> ids);

    PageResponse<ArtistResponse> search(String keyword, int page, int size);

    ArtistDetailResponse getDetail(Integer id);

    ArtistResponse getMine();

    ArtistResponse update(Integer id, ArtistUpdateRequest request);

    void delete(Integer id);

    ArtistResponse createProfile(CreateArtistProfileRequest request);

    ArtistStatsResponse stats(Integer artistId);

    /** Nghệ sĩ gắn với user đang đăng nhập; ném 403 nếu user không phải nghệ sĩ. */
    Artist requireCurrentArtist();

    Artist findActive(Integer id);

    void refreshCounters(Artist artist);
}
