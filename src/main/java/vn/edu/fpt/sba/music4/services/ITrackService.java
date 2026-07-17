package vn.edu.fpt.sba.music4.services;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.sba.music4.dtos.TrackUploadDTO;
import vn.edu.fpt.sba.music4.dtos.response.AlbumBulkJsonDTO;
import vn.edu.fpt.sba.music4.dtos.response.TrackDetailResponseDTO;
import vn.edu.fpt.sba.music4.dtos.response.TrackResponseDTO;
import vn.edu.fpt.sba.music4.entities.Track;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ITrackService {
    // Luồng thêm mới (Upload)[cite: 1, 6]
    TrackDetailResponseDTO uploadTrack(MultipartFile audioFile, MultipartFile coverImage, TrackUploadDTO dto, String currentUsername);

    void uploadMultipleTracks(List<MultipartFile> audioFiles, MultipartFile coverImage, TrackUploadDTO dto);

    // Luồng tìm kiếm/hiển thị phục vụ quản lý của Thành viên 5
    TrackDetailResponseDTO getTrackById(Integer id);

    List<TrackResponseDTO> getTracksByAlbumId(Integer albumId);

    Page<TrackDetailResponseDTO> getAllTrack(Pageable pageable);

    List<TrackDetailResponseDTO> getAllTrack();

    // Luồng chỉnh sửa và xóa metadata bài hát[cite: 6]
    TrackDetailResponseDTO updateTrackMetadata(Integer id, TrackUploadDTO dto, String currentUsername);

    void deleteTrack(Integer id, String currentUsername);

    Integer getAudioDurationInSeconds(MultipartFile audioFile);

    void saveBulkTracksJson(AlbumBulkJsonDTO dto);

    List<TrackDetailResponseDTO> getTop5MostViewedTracks();

    List<TrackResponseDTO> getTracksByArtistId(Integer artistId);

    Page<TrackResponseDTO> getRandomTracks(int seed, Pageable pageable);
}
