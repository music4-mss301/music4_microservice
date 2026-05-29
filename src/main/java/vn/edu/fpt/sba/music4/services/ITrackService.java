package vn.edu.fpt.sba.music4.services;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.sba.music4.dtos.TrackUploadDTO;
import vn.edu.fpt.sba.music4.dtos.response.TrackDetailResponseDTO;
import vn.edu.fpt.sba.music4.dtos.response.TrackResponseDTO;
import vn.edu.fpt.sba.music4.entities.Track;

import java.util.List;

public interface ITrackService {
    // Luồng thêm mới (Upload)[cite: 1, 6]
    TrackDetailResponseDTO uploadTrack(MultipartFile audioFile, MultipartFile coverImage, TrackUploadDTO dto);
    void uploadMultipleTracks(List<MultipartFile> audioFiles, MultipartFile coverImage, TrackUploadDTO dto);
    // Luồng tìm kiếm/hiển thị phục vụ quản lý của Thành viên 5
    TrackDetailResponseDTO getTrackById(Integer id);
    List<TrackResponseDTO> getTracksByAlbumId(Integer albumId);
    List<TrackResponseDTO> getAllTrack();
    // Luồng chỉnh sửa và xóa metadata bài hát[cite: 6]
    TrackDetailResponseDTO updateTrackMetadata(Integer id, TrackUploadDTO dto);
    void deleteTrack(Integer id);
}
