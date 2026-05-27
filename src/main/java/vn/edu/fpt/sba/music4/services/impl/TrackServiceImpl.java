package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.sba.music4.dtos.TrackUploadDTO;
import vn.edu.fpt.sba.music4.entities.*;
import vn.edu.fpt.sba.music4.repositories.*;
import vn.edu.fpt.sba.music4.services.ITrackService;
import vn.edu.fpt.sba.music4.util.AWSUtil;

import java.util.List;

@Service
public class TrackServiceImpl implements ITrackService {
    private TrackRepository trackRepository;
    private AlbumRepository albumRepository;
    private CategoryRepository categoryRepository;
    private TrackCategoryRepository trackCategoryRepository;
    private TrackArtistRepository trackArtistRepository;
    private ArtistRepository artistRepository;
    private final AWSUtil awsUtil;

    public TrackServiceImpl(TrackRepository trackRepository,
                            AlbumRepository albumRepository,
                            CategoryRepository categoryRepository,
                            TrackCategoryRepository trackCategoryRepository,
                            TrackArtistRepository trackArtistRepository,
                            ArtistRepository artistRepository,
                            AWSUtil awsUtil) {
        this.trackRepository = trackRepository;
        this.albumRepository = albumRepository;
        this.categoryRepository = categoryRepository;
        this.trackCategoryRepository = trackCategoryRepository;
        this.trackArtistRepository = trackArtistRepository;
        this.artistRepository = artistRepository;
        this.awsUtil = awsUtil;
    }

    // @Autowired
    // private FileStorageService fileStorageService; // Gọi ké service của Thành viên 1[cite: 7]
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadTrack(MultipartFile audioFile, MultipartFile coverImage, TrackUploadDTO dto) {
        // 🚨 Business Rule: Chặn ngay nếu không có ảnh bìa
        if (coverImage == null || coverImage.isEmpty()) {
            throw new IllegalArgumentException("Ảnh bìa là bắt buộc! Vui lòng đính kèm ảnh bìa.");
        }

        try {
            String imageS3Key = awsUtil.uploadFile(coverImage, "images");
            String audioS3Key = awsUtil.uploadFile(audioFile, "tracks");

            Track track = new Track();
            track.setName(dto.getTitle());
            track.setDuration(dto.getDuration());
            track.setFilePath(audioS3Key);
            track.setPreviewPath(audioS3Key);
            track.setImg(imageS3Key);
            track.setViewCount(0);

            if (dto.getAlbumId() != null) {
                Album album = albumRepository.findById(dto.getAlbumId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Album ID: " + dto.getAlbumId()));
                track.setAlbum(album);
            }

            Track savedTrack = trackRepository.save(track);

            // --- LƯU CATEGORY VÀ ARTIST (Giữ nguyên logic của ông) ---
            saveTrackCategories(savedTrack, dto.getCategoryIds());
            saveTrackArtists(savedTrack, dto.getArtistIds());

        } catch (Exception e) {
            throw new RuntimeException("Upload thất bại: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadMultipleTracks(List<MultipartFile> audioFiles, MultipartFile coverImage, TrackUploadDTO dto) {

        if (coverImage == null || coverImage.isEmpty()) {
            throw new IllegalArgumentException("Ảnh bìa là bắt buộc cho playlist/album này!");
        }
        if (audioFiles == null || audioFiles.isEmpty()) {
            throw new IllegalArgumentException("Phải có ít nhất 1 file âm thanh!");
        }

        try {
            // Tối ưu: Chỉ up ảnh bìa ĐÚNG 1 LẦN lên S3, các bài hát sẽ xài chung cái link này
            String imageS3Key = awsUtil.uploadFile(coverImage, "images");

            // Vòng lặp duyệt qua từng file nhạc người dùng gửi lên
            for (MultipartFile audioFile : audioFiles) {
                String audioS3Key = awsUtil.uploadFile(audioFile, "tracks");

                Track track = new Track();

                // Mẹo: Lấy luôn tên file gốc (bỏ đuôi .mp3) làm Tên bài hát cho nhanh
                // Ví dụ: file "01 - Muộn Rồi Mà Sao Còn.mp3" -> Tên bài: "01 - Muộn Rồi Mà Sao Còn"
                String originalName = audioFile.getOriginalFilename();
                String trackName = (originalName != null) ? originalName.replaceAll("\\.[^.]*$", "") : "Unknown Track";

                track.setName(trackName);
                track.setDuration(0); // Vì up hàng loạt nên chưa rảnh lấy duration từng bài, set 0 tạm
                track.setFilePath(audioS3Key);
                track.setPreviewPath(audioS3Key);
                track.setImg(imageS3Key); // Gắn link ảnh chung vào đây
                track.setViewCount(0);

                if (dto.getAlbumId() != null) {
                    Album album = albumRepository.findById(dto.getAlbumId())
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy Album ID: " + dto.getAlbumId()));
                    track.setAlbum(album);
                }

                Track savedTrack = trackRepository.save(track);

                saveTrackCategories(savedTrack, dto.getCategoryIds());
                saveTrackArtists(savedTrack, dto.getArtistIds());
            }

        } catch (Exception e) {
            throw new RuntimeException("Upload hàng loạt thất bại: " + e.getMessage());
        }
    }

    private void saveTrackCategories(Track track, List<Integer> categoryIds) {
        if (categoryIds != null && !categoryIds.isEmpty()) {
            for (Integer catId : categoryIds) {
                Category category = categoryRepository.findById(catId)
                        .orElseThrow(() -> new RuntimeException("Không tồn tại Thể loại ID: " + catId));
                TrackCategory trackCategory = new TrackCategory();
                trackCategory.setTrack(track);
                trackCategory.setCategory(category);
                trackCategoryRepository.save(trackCategory);
            }
        }
    }

    private void saveTrackArtists(Track track, List<Integer> artistIds) {
        if (artistIds != null && !artistIds.isEmpty()) {
            for (int i = 0; i < artistIds.size(); i++) {
                Integer currentArtistId = artistIds.get(i);
                Artist artist = artistRepository.findById(currentArtistId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Ca sĩ ID: " + currentArtistId));
                TrackArtist trackArtist = new TrackArtist();
                trackArtist.setTrack(track);
                trackArtist.setArtist(artist);
                trackArtist.setRole(i == 0 ? "MAIN" : "FEAT");
                trackArtistRepository.save(trackArtist);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Track getTrackById(Integer id) {
        return trackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài hát với ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Track> getTracksByAlbumId(Integer albumId) {
        // Kiểm tra xem album có tồn tại hay không trước khi tìm kiếm
        if (!albumRepository.existsById(albumId)) {
            throw new RuntimeException("Không tìm thấy Album với ID: " + albumId);
        }
        // Tận dụng quan hệ JPA: Tìm album rồi bốc danh sách bài hát thuộc album đó ra
        Album album = albumRepository.findById(albumId).orElse(null);
        return album != null ? album.getTracks() : List.of();
    }

    @Override
    @Transactional
    public Track updateTrackMetadata(Integer id, TrackUploadDTO dto) {
        Track existingTrack = getTrackById(id);

        // 1. Cập nhật các thông tin cơ bản của bài hát
        existingTrack.setName(dto.getTitle());
        existingTrack.setDuration(dto.getDuration());

        // 2. Cập nhật Album
        if (dto.getAlbumId() != null) {
            Album album = albumRepository.findById(dto.getAlbumId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Album với ID: " + dto.getAlbumId()));
            existingTrack.setAlbum(album);
        } else {
            existingTrack.setAlbum(null); // Bài hát đơn lẻ, không thuộc album
        }

        Track updatedTrack = trackRepository.save(existingTrack);

        // 3. Cập nhật lại Thể loại (Xóa các liên kết cũ trong bảng trung gian và nạp liên kết mới)
        // Lưu ý: Mẹo mượn tính năng mồ côi orphanRemoval = true ở Entity giúp việc làm sạch bảng trung gian rất gọn
        updatedTrack.getTrackCategories().clear();
        if (dto.getCategoryIds() != null) {
            for (Integer catId : dto.getCategoryIds()) {
                Category category = categoryRepository.findById(catId)
                        .orElseThrow(() -> new RuntimeException("Không tồn tại Thể loại ID: " + catId));
                TrackCategory trackCategory = new TrackCategory();
                trackCategory.setTrack(updatedTrack);
                trackCategory.setCategory(category);
                updatedTrack.getTrackCategories().add(trackCategory);
            }
        }

        // 4. Cập nhật lại Nghệ sĩ phối hợp (MAIN & FEAT)
        updatedTrack.getTrackArtists().clear();
        if (dto.getArtistIds() != null) {
            for (int i = 0; i < dto.getArtistIds().size(); i++) {
                Integer artistId = dto.getArtistIds().get(i);
                Artist artist = artistRepository.findById(artistId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Ca sĩ với ID: " + artistId));

                TrackArtist trackArtist = new TrackArtist();
                trackArtist.setTrack(updatedTrack);
                trackArtist.setArtist(artist);
                trackArtist.setRole(i == 0 ? "MAIN" : "FEAT");

                updatedTrack.getTrackArtists().add(trackArtist);
            }
        }

        return trackRepository.save(updatedTrack);
    }

    @Override
    @Transactional
    public void deleteTrack(Integer id) {
        Track track = getTrackById(id);
        // Nhờ cấu hình CascadeType.ALL và orphanRemoval = true ở thực thể Track,
        // lệnh xóa bài hát này sẽ tự động dọn sạch các dòng liên quan trong bảng TRACK_CATEGORIES và TRACK_ARTISTS
        trackRepository.delete(track);
    }
}