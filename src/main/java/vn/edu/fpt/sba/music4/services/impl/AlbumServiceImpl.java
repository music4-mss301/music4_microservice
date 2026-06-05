package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.sba.music4.dtos.response.AlbumCreateDTO;
import vn.edu.fpt.sba.music4.dtos.response.AlbumResponseDTO;
import vn.edu.fpt.sba.music4.dtos.response.TrackResponseDTO;
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

        // 2. Xử lý lưu file ảnh để lấy chuỗi Key/Path
        String savedImageKey = null;
        if (dto.coverImageKey() != null && !dto.coverImageKey().isEmpty()) {
            // 🔥 GỌI HÀM UPLOAD CỦA BẠN (Ví dụ upload lên S3 hoặc lưu vào thư mục local)
            // savedImageKey = s3Service.uploadFile(dto.coverImageFile());

            // Đoạn này tui giả định tên file để bạn dễ hình dung:
            savedImageKey = "albums/covers/" + dto.coverImageKey().getOriginalFilename();
        }

        // 3. Tạo Album mới và gán chuỗi Key vừa có vào
        Album newAlbum = new Album();
        newAlbum.setName(dto.albumTitle());
        newAlbum.setImg(savedImageKey); // 🔥 Lưu chuỗi String vào Database
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
