package vn.edu.fpt.sba.music4.services.impl;

import com.mpatric.mp3agic.Mp3File;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.sba.music4.dtos.TrackUploadDTO;
import vn.edu.fpt.sba.music4.dtos.response.*;
import vn.edu.fpt.sba.music4.entities.*;
import vn.edu.fpt.sba.music4.repositories.*;
import vn.edu.fpt.sba.music4.services.ITrackService;
import vn.edu.fpt.sba.music4.util.AWSUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
            Integer autoDuration = this.getAudioDurationInSeconds(audioFile);
            track.setDuration(autoDuration);
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
            // 1. Tối ưu: Chỉ up ảnh bìa ĐÚNG 1 LẦN lên S3 bên ngoài vòng lặp
            String imageS3Key = awsUtil.uploadFile(coverImage, "images");

            // 2. Tối ưu DB: Tìm Album một lần duy nhất ngoài vòng lặp nếu có truyền albumId
            Album targetAlbum = null;
            if (dto.getAlbumId() != null) {
                targetAlbum = albumRepository.findById(dto.getAlbumId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Album ID: " + dto.getAlbumId()));
            }

            // 3. Xử lý gom danh sách nghệ sĩ an toàn (Giống logic hàm uploadTrack lẻ)
            List<Integer> finalArtistIds = new ArrayList<>();
            if (dto.getArtistId() != null) {
                finalArtistIds.add(dto.getArtistId());
            }
            if (dto.getArtistIds() != null && !dto.getArtistIds().isEmpty()) {
                for (Integer id : dto.getArtistIds()) {
                    if (!finalArtistIds.contains(id)) {
                        finalArtistIds.add(id);
                    }
                }
            }
            if (finalArtistIds.isEmpty()) {
                throw new IllegalArgumentException("Bài hát bắt buộc phải có ít nhất một Ca sĩ/Nghệ sĩ thể hiện!");
            }

            // 4. Vòng lặp duyệt qua từng file nhạc để xử lý lưu trữ
            for (MultipartFile audioFile : audioFiles) {
                String audioS3Key = awsUtil.uploadFile(audioFile, "tracks");

                Track track = new Track();

                // Lấy tên file gốc (bỏ đuôi định dạng) làm tên bài hát
                String originalName = audioFile.getOriginalFilename();
                String trackName = (originalName != null) ? originalName.replaceAll("\\.[^.]*$", "") : "Unknown Track";

                track.setName(trackName);
                track.setDuration(this.getAudioDurationInSeconds(audioFile)); // Tự động lấy số giây
                track.setFilePath(audioS3Key);
                track.setPreviewPath(audioS3Key);
                track.setImg(imageS3Key); // Dùng chung link ảnh bìa
                track.setViewCount(0);

                // Gán trực tiếp đối tượng Album đã tìm ở trên (Không gọi findById lại nữa)
                if (targetAlbum != null) {
                    track.setAlbum(targetAlbum);
                }

                // Lưu bài hát xuống DB
                Track savedTrack = trackRepository.save(track);

                // Lưu các bảng trung gian liên quan
                saveTrackCategories(savedTrack, dto.getCategoryIds());

                // Truyền mảng nghệ sĩ đã gom chuẩn vào đây (Hàm này cũng tự động cộng trackTotal cho Nghệ sĩ luôn rồi)
                saveTrackArtists(savedTrack, finalArtistIds);
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

        Album oldAlbum = existingTrack.getAlbum();

        // Tạo một Set để hứng tất cả ID nghệ sĩ bị ảnh hưởng (cả cũ lẫn mới) để đếm lại ở cuối hàm
        java.util.Set<Integer> affectedArtistIds = new java.util.HashSet<>();

        // 2. Cập nhật thông tin cơ bản
        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            existingTrack.setName(dto.getTitle());
        }
        if (dto.getDuration() != null && dto.getDuration() > 0) {
            existingTrack.setDuration(dto.getDuration());
        }

        // 3. Cập nhật mối quan hệ Album
        Album newAlbum = oldAlbum;
        if (dto.getAlbumId() != null) {
            if (dto.getAlbumId() == -1) {
                existingTrack.setAlbum(null);
                newAlbum = null;
            } else {
                newAlbum = albumRepository.findById(dto.getAlbumId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Album với ID: " + dto.getAlbumId()));
                existingTrack.setAlbum(newAlbum);
            }
        }

        // 4. Cập nhật lại Thể loại
        if (dto.getCategoryIds() != null) {
            existingTrack.getTrackCategories().clear();
            for (Integer catId : dto.getCategoryIds()) {
                Category category = categoryRepository.findById(catId)
                        .orElseThrow(() -> new RuntimeException("Không tồn tại Thể loại ID: " + catId));

                TrackCategory trackCategory = new TrackCategory();
                trackCategory.setTrack(existingTrack);
                trackCategory.setCategory(category);
                existingTrack.getTrackCategories().add(trackCategory);
            }
        }

        // 5. 🛠️ GOM DANH SÁCH NGHỆ SĨ VÀ PHÂN VAI TRÒ (MAIN/FEAT)
        if (dto.getArtistId() != null || dto.getArtistIds() != null) {

            // 🔥 BƯỚC MỚI: Thu thập ID của các nghệ sĩ CŨ trước khi clear sạch bảng trung gian
            for (TrackArtist ta : existingTrack.getTrackArtists()) {
                affectedArtistIds.add(ta.getArtist().getId());
            }

            Integer currentMainArtistId = existingTrack.getTrackArtists().stream()
                    .filter(ta -> "MAIN".equals(ta.getRole()))
                    .map(ta -> ta.getArtist().getId())
                    .findFirst().orElse(null);

            java.util.List<Integer> currentCollabIds = existingTrack.getTrackArtists().stream()
                    .filter(ta -> "FEAT".equals(ta.getRole()))
                    .map(ta -> ta.getArtist().getId())
                    .collect(java.util.stream.Collectors.toList());

            java.util.List<Integer> finalArtistIds = new java.util.ArrayList<>();

            Integer mainArtistId = dto.getArtistId() != null ? dto.getArtistId() : currentMainArtistId;
            if (mainArtistId != null) {
                finalArtistIds.add(mainArtistId);
            }

            java.util.List<Integer> inputCollabs = dto.getArtistIds() != null ? dto.getArtistIds() : currentCollabIds;
            if (inputCollabs != null) {
                for (Integer artistId : inputCollabs) {
                    if (!finalArtistIds.contains(artistId)) {
                        finalArtistIds.add(artistId);
                    }
                }
            }

            if (finalArtistIds.isEmpty()) {
                throw new IllegalArgumentException("Bài hát bắt buộc phải có ít nhất một Ca sĩ/Nghệ sĩ thể hiện!");
            }

            // 🔥 BƯỚC MỚI: Thu thập tiếp ID của các nghệ sĩ MỚI sau khi đã lọc trùng xong
            affectedArtistIds.addAll(finalArtistIds);

            // Dọn dẹp bảng trung gian cũ và ép Hibernate đồng bộ ngay lập tức (Tránh lỗi 500 Unique constraint)
            existingTrack.getTrackArtists().clear();
            trackRepository.saveAndFlush(existingTrack);

            // Nạp danh sách nghệ sĩ mới vào
            for (int i = 0; i < finalArtistIds.size(); i++) {
                Integer artistId = finalArtistIds.get(i);
                Artist artist = artistRepository.findById(artistId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Ca sĩ với ID: " + artistId));

                TrackArtist trackArtist = new TrackArtist();
                trackArtist.setTrack(existingTrack);
                trackArtist.setArtist(artist);
                trackArtist.setRole(i == 0 ? "MAIN" : "FEAT");

                existingTrack.getTrackArtists().add(trackArtist);
            }
        }

        // 6. Lưu thực thể bài hát xuống Database trước
        Track savedTrack = trackRepository.save(existingTrack);

        // 7. Cập nhật lại trackTotal cho ALBUM
        if (dto.getAlbumId() != null) {
            if (oldAlbum != null) {
                int realTotalOld = trackRepository.countByAlbumId(oldAlbum.getId());
                oldAlbum.setTrackTotal(realTotalOld);
                albumRepository.save(oldAlbum);
            }
            if (newAlbum != null && (oldAlbum == null || !oldAlbum.getId().equals(newAlbum.getId()))) {
                int realTotalNew = trackRepository.countByAlbumId(newAlbum.getId());
                newAlbum.setTrackTotal(realTotalNew);
                albumRepository.save(newAlbum);
            }
        }

        // 8. 🔥 CẬP NHẬT LẠI TRACK_TOTAL CHO CÁC NGHỆ SĨ BỊ ẢNH HƯỞNG (MỚI THÊM)
        if (!affectedArtistIds.isEmpty()) {
            for (Integer artistId : affectedArtistIds) {
                Artist artist = artistRepository.findById(artistId).orElse(null);
                if (artist != null) {
                    // Đếm xem nghệ sĩ này xuất hiện bao nhiêu lần trong bảng trung gian TRACK_ARTISTS
                    long realTotalArtist = trackArtistRepository.countByArtistId(artistId);
                    artist.setTrackTotal((int) realTotalArtist);
                    artistRepository.save(artist);
                }
            }
        }

        // 9. Trả về DTO sạch cho Controller
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
                    artistRepository.save(artist); // Cập nhật lại số lượng mới xuống db
                }
            }
        }

        // 3. Tiến hành xóa bài hát (Cascade sẽ tự động dọn dẹp liên kết ở các bảng trung gian)
        trackRepository.delete(track);
    }

    /**
     * Hàm bổ trợ tự động bóc tách thời lượng (giây) từ file nhạc MP3 gửi lên
     */
    @Override
    public Integer getAudioDurationInSeconds(MultipartFile audioFile) {
        File tempFile = null;
        try {
            // 1. Tạo một file tạm trên hệ thống tránh chiếm dụng bộ nhớ lâu dài
            tempFile = File.createTempFile("upload-track-", ".mp3");

            // 2. Ghi dữ liệu từ MultipartFile vào file tạm đó
            audioFile.transferTo(tempFile);

            // 3. Sử dụng mp3agic để đọc thông tin file
            Mp3File mp3File = new Mp3File(tempFile);

            // 4. Lấy ra tổng số giây của bài hát
            long durationSeconds = mp3File.getLengthInSeconds();

            return (int) durationSeconds;
        } catch (Exception e) {
            // Nếu file lỗi hoặc không đúng định dạng MP3 chuẩn, log ra lỗi và trả về 0 hoặc ném lỗi tùy bạn
            System.err.println("Không thể lấy thời lượng file: " + e.getMessage());
            return 0;
        } finally {
            // 🔥 QUAN TRỌNG: Xóa file tạm ngay sau khi dùng xong để tránh rác server
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBulkTracksJson(AlbumBulkJsonDTO dto) {
        if (dto.getTracks() == null || dto.getTracks().isEmpty()) {
            throw new IllegalArgumentException("Album phải có ít nhất 1 bài hát!");
        }

        // 1. Tìm Album dùng chung ngoài vòng lặp (Tối ưu DB như cũ)
        Album targetAlbum = null;
        if (dto.getAlbumId() != null) {
            targetAlbum = albumRepository.findById(dto.getAlbumId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Album ID: " + dto.getAlbumId()));
        }

        // 2. Duyệt qua từng bài hát dạng JSON trong danh sách
        for (TrackJsonMetadataDTO trackDto : dto.getTracks()) {
            Track track = new Track();

            track.setName(trackDto.getTitle());
            track.setDuration(trackDto.getDuration()); // Lấy số giây Frontend gửi lên
            track.setFilePath(trackDto.getAudioFileKey()); // Link nhạc S3
            track.setPreviewPath(trackDto.getAudioFileKey());
            track.setImg(dto.getCoverImageKey()); // Dùng chung link ảnh bìa của Album
            track.setViewCount(0);

            if (targetAlbum != null) {
                track.setAlbum(targetAlbum);
            }

            // Lưu bài hát xuống DB
            Track savedTrack = trackRepository.save(track);

            // 3. TỰ ĐỘNG GOM NGHỆ SĨ RIÊNG CHO TỪNG BÀI HÁT CHÍNH XÁC 100%
            List<Integer> finalArtistIds = new ArrayList<>();
            if (trackDto.getArtistId() != null) {
                finalArtistIds.add(trackDto.getArtistId());
            }
            if (trackDto.getArtistIds() != null) {
                for (Integer id : trackDto.getArtistIds()) {
                    if (!finalArtistIds.contains(id)) finalArtistIds.add(id);
                }
            }

            // 4. Tái sử dụng lại toàn bộ các hàm logic cũ của bạn (Không cần sửa đổi gì các hàm này)
            saveTrackCategories(savedTrack, trackDto.getCategoryIds());
            saveTrackArtists(savedTrack, finalArtistIds); // Hàm này tự tăng trackTotal cho từng Artist luôn!
        }
        // 🔥 CẬP NHẬT TRACK_TOTAL CHO ALBUM (ĐẶT Ở ĐÂY LÀ CHUẨN)
        if (targetAlbum != null) {
            // Gọi TrackRepository đếm thực tế xem Album này hiện đang có tổng cộng bao nhiêu bài hát
            int realTotalTracks = trackRepository.countByAlbumId(targetAlbum.getId());

            // Gán số lượng chuẩn vừa đếm được vào entity Album
            targetAlbum.setTrackTotal(realTotalTracks);

            // Lưu lại thông tin Album mới
            albumRepository.save(targetAlbum);
        }
    }

    @Override
    public List<TrackResponseDTO> getTop5MostViewedTracks() {
        // 1. Gọi Repo lấy 5 bài hát view cao nhất
        List<Track> topTracks = trackRepository.findTop5ByOrderByViewCountDesc();

        // 2. Chuyển đổi List<Track> sang List<TrackResponseDTO>
        return topTracks.stream()
                .map(track -> new TrackResponseDTO(
                        track.getId(),
                        track.getName(),
                        track.getImg(),
                        track.getDuration(),
                        track.getFilePath(),
                        track.getPreviewPath(),
                        track.getViewCount(),
                        track.getUploadDate()

                ))
                .collect(Collectors.toList());
    }
}