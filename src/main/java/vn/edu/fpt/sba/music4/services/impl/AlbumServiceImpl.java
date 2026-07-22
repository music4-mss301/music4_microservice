package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.sba.music4.dtos.response.*;
import vn.edu.fpt.sba.music4.entities.Album;
import vn.edu.fpt.sba.music4.entities.Artist;
import vn.edu.fpt.sba.music4.entities.Notification;
import vn.edu.fpt.sba.music4.entities.Track;
import vn.edu.fpt.sba.music4.exceptions.BusinessException;
import vn.edu.fpt.sba.music4.repositories.*;
import vn.edu.fpt.sba.music4.services.IAlbumService;
import vn.edu.fpt.sba.music4.services.INotificationService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlbumServiceImpl implements IAlbumService {
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final TrackRepository trackRepository;
    private final FollowRepository followRepository;
    private final INotificationService notificationService;
    private final NotificationRepository notificationRepository;

    public AlbumServiceImpl(AlbumRepository albumRepository,
                            ArtistRepository artistRepository,
                            TrackRepository trackRepository,
                            FollowRepository followRepository,
                            NotificationRepository notificationRepository,
                            INotificationService notificationService) {
        this.albumRepository = albumRepository;
        this.artistRepository = artistRepository;
        this.trackRepository = trackRepository;
        this.followRepository = followRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
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

        if (dto.coverImageKey() != null && !dto.coverImageKey().trim().isEmpty()) {
            savedImageKey = dto.coverImageKey();
            if (!savedImageKey.startsWith("images/")) {
                savedImageKey = "images/" + savedImageKey;
            }
        } else {
            savedImageKey = "images/default-cover.png";
        }

        // 3. Tạo Album mới và gán chuỗi Key vào trường 'img'
        Album newAlbum = new Album();
        newAlbum.setName(dto.albumTitle());
        newAlbum.setImg(savedImageKey);
        newAlbum.setArtist(mainArtist);
        newAlbum.setTrackTotal(0);
        newAlbum.setUploadDate(LocalDate.now());

        Album savedAlbum = albumRepository.save(newAlbum);

        // 4. Cập nhật số lượng Album cho Ca sĩ chính
        int realTotalAlbums = albumRepository.countByArtistId(mainArtist.getId());
        mainArtist.setAlbumTotal(realTotalAlbums);
        artistRepository.save(mainArtist);

        // 5. Gửi thông báo real-time đến tất cả người theo dõi nghệ sĩ
        try {
            if (notificationService instanceof NotificationServiceImpl impl) {
                impl.sendNewAlbumNotification(savedAlbum); // 👈 Gọi đúng hàm chuẩn chuyên xử lý album
            }
        } catch (Exception e) {
            System.err.println("Lỗi gửi thông báo tạo album cho người theo dõi: " + e.getMessage());
        }

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

        ArtistResponseDTO artistDTO = null;
        if (album.getArtist() != null) {
            artistDTO = new ArtistResponseDTO(
                    album.getArtist().getId(),
                    album.getArtist().getName(),
                    album.getArtist().getImg(),
                    album.getArtist().getCover(),
                    album.getArtist().getEmail(),
                    album.getArtist().getUsername(),
                    album.getArtist().getTrackTotal(),
                    album.getArtist().getAlbumTotal()
            );
        }

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
    @Transactional(readOnly = true)
    public Page<AlbumDetailResponseDTO> findAll(Pageable pageable) {
        Page<Album> page = albumRepository.findAllWithArtist(pageable);
        return page.map(album -> this.toDetailResponseDto(album));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlbum(Integer id) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Không thể xóa! Không tìm thấy Album có ID: " + id));
        notificationRepository.deleteByAlbumId(id);

        // 1. Xử lý bảng NOTIFICATIONS: Gỡ album_id thành null để tránh lỗi khóa ngoại
        List<Notification> notifications = notificationRepository.findByAlbumId(id);
        if (notifications != null && !notifications.isEmpty()) {
            for (Notification notif : notifications) {
                notif.setAlbum(null);
            }
            notificationRepository.saveAll(notifications);
        }

        // 2. Xử lý bảng TRACKS: Gỡ album_id thành null để biến các bài hát trong album thành Single độc lập
        List<Track> tracksInAlbum = trackRepository.findByAlbumId(id);
        if (tracksInAlbum != null && !tracksInAlbum.isEmpty()) {
            for (Track track : tracksInAlbum) {
                track.setAlbum(null);
            }
            trackRepository.saveAll(tracksInAlbum);
        }

        if (album.getTracks() != null) {
            album.getTracks().clear();
        }

        // 3. Tiến hành xóa Album an toàn
        albumRepository.delete(album);

        // 4. Cập nhật lại tổng số album cho nghệ sĩ
        Artist artist = album.getArtist();
        if (artist != null) {
            int realTotalAlbums = albumRepository.countByArtistId(artist.getId());
            artist.setAlbumTotal(realTotalAlbums);
            artistRepository.save(artist);
        }
    }
}