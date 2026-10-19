package vn.edu.fpt.mss.contentservice.service;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.mss.contentservice.dto.request.AlbumRequest;
import vn.edu.fpt.mss.contentservice.dto.response.AlbumDetailResponse;
import vn.edu.fpt.mss.contentservice.dto.response.AlbumResponse;
import vn.edu.fpt.mss.contentservice.dto.response.PageResponse;

import java.util.List;

public interface AlbumService {
    PageResponse<AlbumResponse> getAll(int page, int size);

    AlbumDetailResponse getDetail(Integer id);

    List<AlbumResponse> getByArtist(Integer artistId);

    AlbumResponse create(AlbumRequest request);

    AlbumResponse createWithCover(String title, MultipartFile cover, Integer artistId);

    AlbumResponse update(Integer id, AlbumRequest request);

    void delete(Integer id);
}
