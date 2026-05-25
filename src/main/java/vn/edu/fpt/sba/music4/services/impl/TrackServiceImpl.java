package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.sba.music4.dtos.TrackUploadDTO;
import vn.edu.fpt.sba.music4.entities.*;
import vn.edu.fpt.sba.music4.repositories.*;
import vn.edu.fpt.sba.music4.services.ITrackService;

import java.util.List;

@Service
public class TrackServiceImpl implements ITrackService {
    private TrackRepository trackRepository;
    private AlbumRepository albumRepository;
    private CategoryRepository categoryRepository;
    private TrackCategoryRepository trackCategoryRepository;
    private TrackArtistRepository trackArtistRepository;
    private ArtistRepository artistRepository;

    public TrackServiceImpl(TrackRepository trackRepository,
                            AlbumRepository albumRepository,
                            CategoryRepository categoryRepository,
                            TrackCategoryRepository trackCategoryRepository,
                            TrackArtistRepository trackArtistRepository,
                            ArtistRepository artistRepository) {
        this.trackRepository = trackRepository;
        this.albumRepository = albumRepository;
        this.categoryRepository = categoryRepository;
        this.trackCategoryRepository = trackCategoryRepository;
        this.trackArtistRepository = trackArtistRepository;
        this.artistRepository = artistRepository;
    }

    // @Autowired
    // private FileStorageService fileStorageService; // Gọi ké service của Thành viên 1[cite: 7]

    @Override
    @Transactional(rollbackFor = Exception.class) //[cite: 7]
    public void uploadTrack(MultipartFile audioFile, MultipartFile coverImage, TrackUploadDTO dto) {

        // BƯỚC 1: Xử lý Upload file qua S3 (Tạm thời giả lập)[cite: 7]
        String audioUrl = "https://amazon-s3-bucket.com/tracks/fake-audio-path.mp3"; //[cite: 7]
        String imageUrl = "https://amazon-s3-bucket.com/images/fake-cover-path.jpg"; //[cite: 7]

        // BƯỚC 2: Tạo và Lưu thông tin vào bảng TRACKS[cite: 7]
        Track track = new Track(); //[cite: 7]
        track.setName(dto.getTitle()); //[cite: 7]
        track.setDuration(dto.getDuration()); //[cite: 7]
        track.setFilePath(audioUrl); //[cite: 7]
        track.setPreviewPath(audioUrl); //[cite: 7]
        track.setViewCount(0); //[cite: 7]

        if (dto.getAlbumId() != null) { //[cite: 7]
            Album album = albumRepository.findById(dto.getAlbumId()) //[cite: 7]
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Album với ID: " + dto.getAlbumId())); //[cite: 7]
            track.setAlbum(album); //[cite: 7]
        } //[cite: 7]

        Track savedTrack = trackRepository.save(track); //[cite: 7]

        // BƯỚC 3: Lưu dữ liệu vào bảng trung gian TRACK_CATEGORIES[cite: 7]
        if (dto.getCategoryIds() != null && !dto.getCategoryIds().isEmpty()) { //[cite: 7]
            for (Integer catId : dto.getCategoryIds()) { //[cite: 7]
                Category category = categoryRepository.findById(catId) //[cite: 7]
                        .orElseThrow(() -> new RuntimeException("Không tồn tại Thể loại ID: " + catId)); //[cite: 7]

                TrackCategory trackCategory = new TrackCategory(); //[cite: 7]
                trackCategory.setTrack(savedTrack); //[cite: 7]
                trackCategory.setCategory(category); //[cite: 7]

                trackCategoryRepository.save(trackCategory); //[cite: 7]
            } //[cite: 7]
        } //[cite: 7]

        // BƯỚC 4: Tìm Artist thật từ DB và lưu vào bảng trung gian TRACK_ARTISTS
        if (dto.getArtistIds() != null && !dto.getArtistIds().isEmpty()) { //[cite: 7]
            for (int i = 0; i < dto.getArtistIds().size(); i++) { //[cite: 7]
                Integer artistId = dto.getArtistIds().get(i); //[cite: 7]

                // Đã chỉnh sửa: Tìm kiếm Artist thật từ DB thay vì tạo đối tượng ảo[cite: 7]
                Artist artist = artistRepository.findById(artistId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy Ca sĩ với ID: " + artistId));

                TrackArtist trackArtist = new TrackArtist(); //[cite: 7]
                trackArtist.setTrack(savedTrack); //[cite: 7]
                trackArtist.setArtist(artist); //[cite: 7]

                if (i == 0) { //[cite: 7]
                    trackArtist.setRole("MAIN"); //[cite: 7]
                } else { //[cite: 7]
                    trackArtist.setRole("FEAT"); //[cite: 7]
                } //[cite: 7]

                trackArtistRepository.save(trackArtist); //[cite: 7]
            } //[cite: 7]
        } //[cite: 7]
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