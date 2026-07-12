package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.sba.music4.dtos.response.*;
import vn.edu.fpt.sba.music4.entities.Album;
import vn.edu.fpt.sba.music4.entities.Artist;
import vn.edu.fpt.sba.music4.entities.Track;
import vn.edu.fpt.sba.music4.exceptions.BusinessException;
import vn.edu.fpt.sba.music4.repositories.AlbumRepository;
import vn.edu.fpt.sba.music4.repositories.ArtistRepository;
import vn.edu.fpt.sba.music4.repositories.TrackRepository;
import vn.edu.fpt.sba.music4.services.IAlbumService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlbumServiceImpl implements IAlbumService {
    private AlbumRepository albumRepository;
    private ArtistRepository artistRepository;
    private TrackRepository trackRepository;

    public AlbumServiceImpl(AlbumRepository albumRepository, ArtistRepository artistRepository, TrackRepository trackRepository) {
        this.albumRepository = albumRepository;
        this.artistRepository = artistRepository;
        this.trackRepository = trackRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlbumResponseDTO> getAlbumsByArtist(Integer artistId) {
        List<Album> albums = albumRepository.findByArtistId(artistId);

        return albums.stream()
                .map(album -> new AlbumResponseDTO(
                        album.getId(),
                        album.getName(),
                        album.getImg(),
                        album.getUploadDate(),
                        album.getTrackTotal(),
                        album.getArtist().getId()
                )).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Album createAlbum(AlbumCreateDTO dto) {
        // 1. Tìm Ca sĩ
        Artist mainArtist = artistRepository.findById(dto.artistId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Ca sĩ ID: " + dto.artistId()));

        // 2. Xử lý chuỗi Key/Path nhận từ Frontend gửi qua
        String savedImageKey = null;

        // Kiểm tra DTO gửi lên có dữ liệu hay không
        if (dto.coverImageKey() != null && !dto.coverImageKey().trim().isEmpty()) {
            savedImageKey = dto.coverImageKey();

            // ✅ BẮT BUỘC NẰM TRONG KHỐI IF: Chỉ gọi startsWith khi savedImageKey CHẮC CHẮN KHÔNG NULL
            if (!savedImageKey.startsWith("images/")) {
                savedImageKey = "images/" + savedImageKey;
            }
        } else {
            // 🔥 PHÒNG THỦ: Nếu trường hợp Frontend gửi lên chuỗi rỗng hoặc null,
            // bồ có thể gán cho nó 1 cái ảnh mặc định để DB không bị trống trường img.
            savedImageKey = "images/default-cover.png";
        }

        // 3. Tạo Album mới và gán chuỗi Key vào trường 'img'
        Album newAlbum = new Album();
        newAlbum.setName(dto.albumTitle());
        newAlbum.setImg(savedImageKey); // Lưu chuỗi String sạch vào Database
        newAlbum.setArtist(mainArtist);
        newAlbum.setTrackTotal(0);
        newAlbum.setUploadDate(LocalDate.now());

        Album savedAlbum = albumRepository.save(newAlbum);

        // 4. Cập nhật số lượng Album cho Ca sĩ chính như cũ
        int realTotalAlbums = albumRepository.countByArtistId(mainArtist.getId());
        mainArtist.setAlbumTotal(realTotalAlbums);
        artistRepository.save(mainArtist);

        return savedAlbum;
    }

    public AlbumResponseDTO toResponseDto(Album album) {
        if (album == null) return null;

        return new AlbumResponseDTO(
                album.getId(),
                album.getName(),
                album.getImg(),
                album.getUploadDate(),
                album.getTrackTotal(),
                album.getArtist().getId()
        );
    }

    public AlbumDetailResponseDTO toDetailResponseDto(Album album) {
        if (album == null) return null;

        // 1. Ánh xạ Artist chủ quản với cấu trúc mới
        ArtistResponseDTO artistDTO = null;
        if (album.getArtist() != null) {
            // Dùng constructor đầy đủ hoặc constructor rút gọn bồ đã định nghĩa
            artistDTO = new ArtistResponseDTO(
                    album.getArtist().getId(),
                    album.getArtist().getName(),
                    album.getArtist().getImg(),
                    album.getArtist().getCover(),
                    album.getArtist().getEmail(),
                    album.getArtist().getUsername(), // Đảm bảo entity Artist có getUserName()
                    album.getArtist().getTrackTotal(), // Đảm bảo entity Artist có getTrackTotal()
                    album.getArtist().getAlbumTotal()  // Đảm bảo entity Artist có getAlbumTotal()
            );
        }

        // 2. Lấy danh sách Tracks bằng cách gọi trực tiếp Repository (như đã sửa ở bước trước)
        List<Track> tracks = trackRepository.findByAlbumId(album.getId());

        List<TrackResponseDTO> trackList = tracks.stream()
                .map(t -> new TrackResponseDTO(
                        t.getId(),
                        t.getName(),
                        album.getName(),
                        t.getImg(),
                        t.getDuration(),
                        t.getFilePath(),
                        t.getPreviewPath(),
                        t.getViewCount(),
                        t.getUploadDate(),
                        0,
                        t.getTrackArtists() != null ? t.getTrackArtists().stream()
                                .map(ta -> new TrackArtistInfo(ta.getArtist().getId(), ta.getArtist().getName(), ta.getRole()))
                                .toList() : java.util.List.of()
                )).toList();

        return new AlbumDetailResponseDTO(
                album.getId(),
                album.getName(),
                album.getImg(),
                album.getUploadDate(),
                album.getTrackTotal(),
                artistDTO,
                trackList
        );
    }

    @Override
    public List<AlbumResponseDTO> findAll() {
        return albumRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true) // <--- THÊM CÁI NÀY VÀO
    public Page<AlbumDetailResponseDTO> findAll(Pageable pageable) {
        Page<Album> page = albumRepository.findAllWithArtist(pageable);
        return page.map(album -> this.toDetailResponseDto(album));
    }

    // ok
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlbum(Integer id) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không thể xóa! Không tìm thấy Album có ID: " + id));

        List<Track> tracksInAlbum = trackRepository.findByAlbumId(id);

        if (tracksInAlbum != null && !tracksInAlbum.isEmpty()) {
            for (Track track : tracksInAlbum) {
                track.setAlbum(null); // Biến bài hát trong album thành bài hát đơn lẻ (Single)
            }
        }
        if (album.getTracks() != null) {
            album.getTracks().clear();
        }

        albumRepository.delete(album);
    }
}
