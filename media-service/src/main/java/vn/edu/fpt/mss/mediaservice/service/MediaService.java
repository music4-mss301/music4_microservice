package vn.edu.fpt.mss.mediaservice.service;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.mss.mediaservice.dto.response.MediaFileResponse;
import vn.edu.fpt.mss.mediaservice.dto.response.MediaUploadResponse;
import vn.edu.fpt.mss.mediaservice.dto.response.PageResponse;

import java.util.List;

public interface MediaService {
    MediaUploadResponse upload(MultipartFile file, String type, Integer uploadedBy);

    MediaFileResponse get(Long id);

    PageResponse<MediaFileResponse> list(String type, int page, int size);

    MediaFileResponse replace(Long id, MultipartFile file);

    void delete(Long id);

    int deleteByKeys(List<String> keys);
}
