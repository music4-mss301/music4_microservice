package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.sba.music4.dtos.response.*;
import vn.edu.fpt.sba.music4.entities.*;
import vn.edu.fpt.sba.music4.repositories.*;
import vn.edu.fpt.sba.music4.services.ISearchService;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class SearchServiceImpl implements ISearchService {
    private final TrackRepository trackRepository;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final PlaylistRepository playlistRepository;
    private final CategoryRepository categoryRepository;
    private final TrackArtistRepository trackArtistRepository;
    public SearchServiceImpl(TrackRepository trackRepository, AlbumRepository albumRepository, ArtistRepository artistRepository,
                             PlaylistRepository playlistRepository,
                             CategoryRepository categoryRepository,
                             TrackArtistRepository trackArtistRepository) {
        this.trackRepository = trackRepository;
        this.albumRepository = albumRepository;
        this.artistRepository = artistRepository;
        this.playlistRepository = playlistRepository;
        this.categoryRepository = categoryRepository;
        this.trackArtistRepository = trackArtistRepository;
    }
    private String cleanKeyword(String keyword) {
        if (keyword == null) return "";

        // Loại bỏ khoảng trắng thừa ở 2 đầu
        String trimmed = keyword.trim();

        // Chỉ chuyển về chữ thường và thay thế dấu '-' thành khoảng trắng.
        // Bỏ logic Normalizer để giữ nguyên dấu tiếng Việt (như ề, à) cho DB xử lý.
        return trimmed.toLowerCase().replace("-", " ");
    }
    private TrackResponseDTO mapToTrackDTO(Track track) {
        String albumName = track.getAlbum() != null ? track.getAlbum().getName() : "Single";

        List<TrackArtistInfo> artists = List.of();

        List<TrackArtist> dbTrackArtists = trackArtistRepository.findByTrackIdWithArtist(track.getId());

        if (dbTrackArtists != null && !dbTrackArtists.isEmpty()) {
            artists = dbTrackArtists.stream()
                    .map(ta -> new TrackArtistInfo(
                            ta.getArtist().getId(),
                            ta.getArtist().getName(),
                            ta.getRole()
                    )).toList();
        }

        return new TrackResponseDTO(
                track.getId(), track.getName(), albumName, track.getImg(),
                track.getDuration(), track.getFilePath(), track.getPreviewPath(),
                track.getViewCount(), track.getUploadDate(),null, artists // Hết bị null rồi nhé!
        );
    }
    @Override
    @Transactional(readOnly = true)
    public SearchResultResponseDTO search(String keyword, String type, int page, int size) {
        String cleanKey = cleanKeyword(keyword);

        // Khởi tạo đối tượng phân trang chung cho tất cả
        Pageable pageable = PageRequest.of(page, size);

        if (type == null || type.equalsIgnoreCase("all")) {
            Page<Track> trackPage = trackRepository.searchByName(cleanKey, pageable);
            Page<Album> albumPage = albumRepository.searchByName(cleanKey, pageable);
            Page<Artist> artistPage = artistRepository.searchByName(cleanKey, pageable);
            Page<Category> categoryPage = categoryRepository.searchByName(cleanKey, pageable);

            Page<TrackResponseDTO> trackDTOPage = trackPage.map(this::mapToTrackDTO);

            Page<AlbumResponseDTO> albumDTOPage = albumPage.map(album -> new AlbumResponseDTO(
                    album.getId(), album.getName(), album.getImg(), album.getUploadDate(), album.getTrackTotal(),
                    album.getArtist() != null ? album.getArtist().getId() : null
            ));

            Page<ArtistResponseDTO> artistDTOPage = artistPage.map(artist -> new ArtistResponseDTO(
                    artist.getId(), artist.getName(), artist.getImg(), artist.getUsername(),artist.getTrackTotal() ,artist.getAlbumTotal()
            ));


            Page<CategoryResponseDTO> categoryDTOPage = categoryPage.map(category -> new CategoryResponseDTO(
                    category.getId(), category.getName()
            ));
            return new SearchResultResponseDTO(trackDTOPage, albumDTOPage, artistDTOPage, categoryDTOPage);
        }

        // 2. TÌM KIẾM CHI TIẾT THEO TYPE
        return switch (type.toLowerCase()) {
            case "track" -> {
                Page<TrackResponseDTO> trackDTOPage = trackRepository.searchByName(cleanKey, pageable).map(this::mapToTrackDTO);
                yield new SearchResultResponseDTO(trackDTOPage,null,null,null); }

            case "artist" -> {
                Page<ArtistResponseDTO> artistDTOPage = artistRepository.searchByName(cleanKey, pageable).map(artist -> new ArtistResponseDTO(
                        artist.getId(), artist.getName(), artist.getImg(), artist.getUsername(),artist.getTrackTotal(),artist.getAlbumTotal()
                ));
                yield new SearchResultResponseDTO(null, null, artistDTOPage,null);
            }

            case "album" -> {
                Page<AlbumResponseDTO> albumDTOPage = albumRepository.searchByName(cleanKey, pageable).map(album -> new AlbumResponseDTO(
                        album.getId(), album.getName(), album.getImg(), album.getUploadDate(), album.getTrackTotal(),
                        album.getArtist() != null ? album.getArtist().getId() : null
                ));
                 yield new SearchResultResponseDTO(null, albumDTOPage, null, null);
            }

            case "category" -> {
                Page<CategoryResponseDTO> categoryDTOPage = categoryRepository.searchByName(cleanKey, pageable).map(category -> new CategoryResponseDTO(
                        category.getId(), category.getName()
                ));
                 yield new SearchResultResponseDTO(null, null, null, categoryDTOPage);
            }
            default -> new SearchResultResponseDTO(null, null, null, null);
        };
    }
}
