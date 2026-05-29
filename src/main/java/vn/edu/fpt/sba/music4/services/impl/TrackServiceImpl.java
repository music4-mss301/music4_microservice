package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.sba.music4.dtos.TrackUploadDTO;
import vn.edu.fpt.sba.music4.dtos.response.*;
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
    public TrackDetailResponseDTO uploadTrack(MultipartFile audioFile, MultipartFile coverImage, TrackUploadDTO dto) {
        // 🚨 Business Rule: Chặn ngay nếu không có ảnh bìa
        if (coverImage == null || coverImage.isEmpty()) {
            throw new IllegalArgumentException("Ảnh bìa là bắt buộc! Vui lòng đính kèm ảnh bìa.");
        }

        try {
            // 1. Upload file lên AWS S3
            String imageS3Key = awsUtil.uploadFile(coverImage, "images");
            String audioS3Key = awsUtil.uploadFile(audioFile, "tracks");

            // 2. Khởi tạo đối tượng Track và map dữ liệu cơ bản
            Track track = new Track();
            track.setName(dto.getTitle());
            track.setDuration(dto.getDuration());
            track.setFilePath(audioS3Key);
            track.setPreviewPath(audioS3Key);
            track.setImg(imageS3Key);
            track.setViewCount(0);

            // 3. Xử lý Album (Nếu có truyền albumId)
            if (dto.getAlbumId() != null) {
                Album album = albumRepository.findById(dto.getAlbumId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Album ID: " + dto.getAlbumId()));
                track.setAlbum(album);
            }

            // 4. Lưu bài hát vào Database trước để lấy savedTrack (đã có ID tự tăng và uploadDate từ @PrePersist)
            Track savedTrack = trackRepository.save(track);

            // 5. Xử lý lưu Thể loại nhạc (Category) vào bảng trung gian
            saveTrackCategories(savedTrack, dto.getCategoryIds());

            // 6. XỬ LÝ LƯU NGHỆ SĨ (TRACK_ARTIST) CHUẨN XÁC VÀ AN TOÀN CHỐNG TRÙNG:
            java.util.List<Integer> finalArtistIds = new java.util.ArrayList<>();

            // Ưu tiên nạp artistId lẻ (ca sĩ chính) vào vị trí đầu tiên để gán vai trò "MAIN"
            if (dto.getArtistId() != null) {
                finalArtistIds.add(dto.getArtistId());
            }

            // Nếu có truyền thêm danh sách artistIds, tiến hành nạp tiếp và lọc trùng dữ liệu
            if (dto.getArtistIds() != null && !dto.getArtistIds().isEmpty()) {
                for (Integer id : dto.getArtistIds()) {
                    if (!finalArtistIds.contains(id)) {
                        finalArtistIds.add(id);
                    }
                }
            }

            // Khống chế: Bài hát bắt buộc phải có ít nhất 1 ca sĩ sở hữu
            if (finalArtistIds.isEmpty()) {
                throw new IllegalArgumentException("Bài hát bắt buộc phải có ít nhất một Ca sĩ/Nghệ sĩ thể hiện!");
            }

            // Gọi hàm để lưu toàn bộ danh sách nghệ sĩ này vào bảng trung gian TrackArtist
            saveTrackArtists(savedTrack, finalArtistIds);

            // 7. 🚨 QUYẾT ĐỊNH: Chuyển đổi sang Record DTO sạch trước khi trả về Controller
            // Sử dụng hàm toDetailResponseDto bạn đã viết ở cuối file để bóc tách sạch sẽ dữ liệu quan hệ vừa lưu thành công!
            return this.toDetailResponseDto(savedTrack);

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
                Integer trackTotal = artist.getTrackTotal();
                artist.setTrackTotal(trackTotal == null ? 1 : trackTotal + 1);
                artistRepository.save(artist);
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
    public TrackDetailResponseDTO getTrackById(Integer id) {
        Track track = trackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài hát với ID: " + id));
        // 2. Ánh xạ Album (Trả về null nếu là bài hát Single không thuộc album nào)
        AlbumResponseDTO albumDTO = null;
        if (track.getAlbum() != null) {
            albumDTO = new AlbumResponseDTO(
                    track.getAlbum().getId(),
                    track.getAlbum().getName(),
                    track.getAlbum().getImg(),
                    track.getAlbum().getUploadDate(),
                    track.getAlbum().getTrackTotal(),
                    track.getAlbum().getArtist().getId()
            );
        }

        // 3. Ánh xạ danh sách Artists từ bảng trung gian TrackArtist sang TrackArtistInfo
        List<TrackArtistInfo> artistList = List.of();
        if (track.getTrackArtists() != null) {
            artistList = track.getTrackArtists().stream()
                    .map(ta -> new TrackArtistInfo(
                            ta.getArtist().getId(),
                            ta.getArtist().getName(),
                            ta.getRole() // Vai trò MAIN hoặc FEAT
                    )).toList();
        }

        // 4. Ánh xạ danh sách Categories từ bảng trung gian TrackCategory sang CategoryResponseDTO
        List<CategoryResponseDTO> categoryList = List.of();
        if (track.getTrackCategories() != null) {
            categoryList = track.getTrackCategories().stream()
                    .map(tc -> new CategoryResponseDTO(
                            tc.getCategory().getId(),
                            tc.getCategory().getName()
                    )).toList();
        }
        return new TrackDetailResponseDTO(
                track.getId(),
                track.getName(),
                track.getImg(),
                track.getDuration(),
                track.getFilePath(),
                track.getPreviewPath(),
                track.getUploadDate(),
                track.getViewCount(),
                albumDTO,
                artistList,
                categoryList
        );
    }

    @Override
    @Transactional
    public List<TrackResponseDTO> getTracksByAlbumId(Integer albumId) {
        // Tìm danh sách Entity từ Repository
        List<Track> tracks = trackRepository.findByAlbumId(albumId);

        // Dùng Stream map toàn bộ danh sách sang phiên bản DTO rút gọn
        return tracks.stream()
                .map(this::toResponseDto)
                .toList();
    }

    public TrackResponseDTO toResponseDto(Track track) {
        if (track == null) return null;

        return new TrackResponseDTO(
                track.getId(),
                track.getName(),
                track.getImg(),
                track.getDuration(),
                track.getFilePath(),
                track.getPreviewPath(),
                track.getViewCount(),
                track.getUploadDate()
        );
    }

    @Override
    public List<TrackResponseDTO> getAllTrack() {
        return trackRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrackDetailResponseDTO updateTrackMetadata(Integer id, TrackUploadDTO dto) {
        // 1. Tìm bài hát hiện tại trong Database
        Track existingTrack = trackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài hát ID: " + id));

        // 2. Cập nhật các thông tin cơ bản của bài hát
        existingTrack.setName(dto.getTitle());
        existingTrack.setDuration(dto.getDuration());

        // 3. Cập nhật mối quan hệ Album (Nếu null nghĩa là chuyển bài hát thành Single lẻ)
        if (dto.getAlbumId() != null) {
            Album album = albumRepository.findById(dto.getAlbumId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Album với ID: " + dto.getAlbumId()));
            existingTrack.setAlbum(album);
        } else {
            existingTrack.setAlbum(null);
        }

        // 4. Cập nhật lại Thể loại (Xóa liên kết cũ, nạp liên kết mới)
        existingTrack.getTrackCategories().clear();
        if (dto.getCategoryIds() != null) {
            for (Integer catId : dto.getCategoryIds()) {
                Category category = categoryRepository.findById(catId)
                        .orElseThrow(() -> new RuntimeException("Không tồn tại Thể loại ID: " + catId));

                TrackCategory trackCategory = new TrackCategory();
                trackCategory.setTrack(existingTrack);
                trackCategory.setCategory(category);
                existingTrack.getTrackCategories().add(trackCategory);
            }
        }

        // 5. 🛠️ GOM DANH SÁCH NGHỆ SĨ AN TOÀN CHỐNG TRÙNG VÀ PHÂN VAI TRÒ (MAIN/FEAT)
        java.util.List<Integer> finalArtistIds = new java.util.ArrayList<>();

        // Ưu tiên nạp artistId lẻ (ca sĩ chính) vào vị trí đầu tiên
        if (dto.getArtistId() != null) {
            finalArtistIds.add(dto.getArtistId());
        }

        // Nạp tiếp danh sách nghệ sĩ collab/phụ nếu có và tiến hành lọc trùng
        if (dto.getArtistIds() != null && !dto.getArtistIds().isEmpty()) {
            for (Integer artistId : dto.getArtistIds()) {
                if (!finalArtistIds.contains(artistId)) {
                    finalArtistIds.add(artistId);
                }
            }
        }

        // Chặn lỗi: Bài hát sau khi cập nhật vẫn bắt buộc phải có ít nhất 1 ca sĩ thể hiện
        if (finalArtistIds.isEmpty()) {
            throw new IllegalArgumentException("Bài hát bắt buộc phải có ít nhất một Ca sĩ/Nghệ sĩ thể hiện!");
        }

        // Tiến hành dọn dẹp bảng trung gian cũ và lưu danh sách mới vào
        existingTrack.getTrackArtists().clear();
        for (int i = 0; i < finalArtistIds.size(); i++) {
            Integer artistId = finalArtistIds.get(i);
            Artist artist = artistRepository.findById(artistId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Ca sĩ với ID: " + artistId));

            TrackArtist trackArtist = new TrackArtist();
            trackArtist.setTrack(existingTrack);
            trackArtist.setArtist(artist);
            trackArtist.setRole(i == 0 ? "MAIN" : "FEAT"); // Vị trí đầu tiên luôn là MAIN

            existingTrack.getTrackArtists().add(trackArtist);
        }

        // 6. Lưu thực thể cuối cùng xuống Database
        Track savedTrack = trackRepository.save(existingTrack);

        // 7. 🚨 QUYẾT ĐỊNH: Chuyển đổi sang Record DTO sạch trước khi trả về Controller
        return this.toDetailResponseDto(savedTrack);
    }

    // 💡 Hàm hỗ trợ 1: Chuyển đổi từ Track sang TrackDetailResponseDTO (Full thông tin chi tiết)
    public TrackDetailResponseDTO toDetailResponseDto(Track track) {
        if (track == null) return null;

        // 1. Ánh xạ Album (Trả về null nếu là bài hát Single không thuộc album nào)
        AlbumResponseDTO albumDTO = null;
        if (track.getAlbum() != null) {
            albumDTO = new AlbumResponseDTO(
                    track.getAlbum().getId(),
                    track.getAlbum().getName(),
                    track.getAlbum().getImg(),
                    track.getAlbum().getUploadDate(),
                    track.getAlbum().getTrackTotal(),
                    track.getAlbum().getArtist().getId()
            );
        }

        // 2. Ánh xạ danh sách Artists từ bảng trung gian TrackArtist
        java.util.List<TrackArtistInfo> artistList = java.util.List.of();
        if (track.getTrackArtists() != null) {
            artistList = track.getTrackArtists().stream()
                    .map(ta -> new TrackArtistInfo(
                            ta.getArtist().getId(),
                            ta.getArtist().getName(),
                            ta.getRole() // Lấy vai trò MAIN hoặc FEAT từ bảng trung gian
                    )).toList();
        }

        // 3. Ánh xạ danh sách Categories từ bảng trung gian TrackCategory
        java.util.List<CategoryResponseDTO> categoryList = java.util.List.of();
        if (track.getTrackCategories() != null) {
            categoryList = track.getTrackCategories().stream()
                    .map(tc -> new CategoryResponseDTO(
                            tc.getCategory().getId(),
                            tc.getCategory().getName()
                    )).toList();
        }

        // 4. Trả về đối tượng Record hoàn chỉnh
        return new TrackDetailResponseDTO(
                track.getId(),
                track.getName(),
                track.getImg(),
                track.getDuration(),
                track.getFilePath(),
                track.getPreviewPath(),
                track.getUploadDate(),
                track.getViewCount(),
                albumDTO,
                artistList,
                categoryList
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTrack(Integer id) {
        // 1. Tìm bài hát xem có tồn tại không trước khi tiến hành xóa
        Track track = trackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài hát ID: " + id));

        // 2. 🔥 GIẢM TRACK TOTAL CỦA CÁC NGHỆ SĨ THAM GIA BÀI HÁT NÀY
        if (track.getTrackArtists() != null) {
            for (TrackArtist trackArtist : track.getTrackArtists()) {
                Artist artist = trackArtist.getArtist();
                Integer currentTotal = artist.getTrackTotal();
                if (currentTotal != null && currentTotal > 0) {
                    artist.setTrackTotal(currentTotal - 1);
                    artistRepository.save(artist); // Cập nhật lại số lượng mới xuống DB
                }
            }
        }

        // 3. Tiến hành xóa bài hát (Cascade sẽ tự động dọn dẹp liên kết ở các bảng trung gian)
        trackRepository.delete(track);
    }
}