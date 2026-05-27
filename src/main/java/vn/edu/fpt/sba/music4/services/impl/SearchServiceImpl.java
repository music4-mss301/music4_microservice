package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.fpt.sba.music4.dtos.response.*;
import vn.edu.fpt.sba.music4.entities.*;
import vn.edu.fpt.sba.music4.repositories.*;
import vn.edu.fpt.sba.music4.services.ISearchService;

import java.text.Normalizer;
import java.util.regex.Pattern;

@Service
public class SearchServiceImpl implements ISearchService {
    private final TrackRepository trackRepository;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final PlaylistRepository playlistRepository;
    private final CategoryRepository categoryRepository;

    public SearchServiceImpl(TrackRepository trackRepository, AlbumRepository albumRepository, ArtistRepository artistRepository, PlaylistRepository playlistRepository, CategoryRepository categoryRepository) {
        this.trackRepository = trackRepository;
        this.albumRepository = albumRepository;
        this.artistRepository = artistRepository;
        this.playlistRepository = playlistRepository;
        this.categoryRepository = categoryRepository;
    }
    private String cleanKeyword(String keyword) {
        if (keyword == null) return "";
        String normalized = Normalizer.normalize(keyword, Normalizer.Form.NFD);
        String noAccent = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(normalized).replaceAll("");
        return noAccent.toLowerCase().replace("-", " ");
    }
    @Override
    public SearchResultResponseDTO search(String keyword, String type, int page, int size) {
        String cleanKey = cleanKeyword(keyword);

        // Khởi tạo đối tượng phân trang chung cho tất cả
        Pageable pageable = PageRequest.of(page, size);

        // 1. TÌM KIẾM TẤT CẢ (Không giới hạn Top 5 nữa, lấy theo đúng size truyền vào)
        if (type == null || type.equalsIgnoreCase("all")) {
            Page<Track> trackPage = trackRepository.searchByName(cleanKey, pageable);
            Page<Album> albumPage = albumRepository.searchByName(cleanKey, pageable);
            Page<Artist> artistPage = artistRepository.searchByName(cleanKey, pageable);
            Page<Playlist> playlistPage = playlistRepository.searchByName(cleanKey, pageable);
            Page<Category> categoryPage = categoryRepository.searchByName(cleanKey, pageable);

            Page<TrackResponseDTO> trackDTOPage = trackPage.map(track -> new TrackResponseDTO(
                    track.getId(), track.getName(), track.getImg(), track.getDuration(), track.getPreviewPath(),track.getViewCount(),track.getUploadDate()
            ));

            Page<AlbumResponseDTO> albumDTOPage = albumPage.map(album -> new AlbumResponseDTO(
                    album.getId(), album.getName(), album.getImg(), album.getUploadDate(), album.getTrackTotal(),
                    album.getArtist() != null ? album.getArtist().getId() : null
            ));

            Page<ArtistResponseDTO> artistDTOPage = artistPage.map(artist -> new ArtistResponseDTO(
                    artist.getId(), artist.getName(), artist.getImg(), artist.getUserName(),artist.getTrackTotal(),artist.getAlbumTotal()
            ));

            Page<PlaylistResponseDTO> playlistDTOPage = playlistPage.map(playlist -> new PlaylistResponseDTO(
                    playlist.getId(), playlist.getName(),
                    playlist.getUser() != null ? playlist.getUser().getId() : null,
                    playlist.getName()
            ));

            Page<CategoryResponseDTO> categoryDTOPage = categoryPage.map(category -> new CategoryResponseDTO(
                    category.getId(), category.getName()
            ));
            return new SearchResultResponseDTO(trackDTOPage, albumDTOPage, artistDTOPage, playlistDTOPage, categoryDTOPage);
        }

        // 2. TÌM KIẾM CHI TIẾT THEO TYPE
        return switch (type.toLowerCase()) {
            case "track" -> {Page<TrackResponseDTO> trackDTOPage = trackRepository.searchByName(cleanKey,pageable).map(track -> new TrackResponseDTO(
                    track.getId(), track.getName(), track.getImg(), track.getDuration(), track.getPreviewPath(),track.getViewCount(),track.getUploadDate()
            ));
            yield new SearchResultResponseDTO(trackDTOPage,null,null,null,null);
            }

            case "artist" -> {
                Page<ArtistResponseDTO> artistDTOPage = artistRepository.searchByName(cleanKey, pageable).map(artist -> new ArtistResponseDTO(
                        artist.getId(), artist.getName(), artist.getImg(), artist.getUserName(),artist.getTrackTotal(),artist.getAlbumTotal()
                ));
                yield new SearchResultResponseDTO(null, null, artistDTOPage, null, null);
            }

            case "album" -> {
                Page<AlbumResponseDTO> albumDTOPage = albumRepository.searchByName(cleanKey, pageable).map(album -> new AlbumResponseDTO(
                        album.getId(), album.getName(), album.getImg(), album.getUploadDate(), album.getTrackTotal(),
                        album.getArtist() != null ? album.getArtist().getId() : null
                ));
                 yield new SearchResultResponseDTO(null, albumDTOPage, null, null, null);
            }
            case "playlist" -> {
                Page<PlaylistResponseDTO> playlistDTOPage = playlistRepository.searchByName(cleanKey, pageable).map(playlist -> new PlaylistResponseDTO(
                        playlist.getId(), playlist.getName(),
                        playlist.getUser() != null ? playlist.getUser().getId() : null,
                        playlist.getName()
                ));
                 yield new SearchResultResponseDTO(null, null, null, playlistDTOPage, null);
            }
            case "category" -> {
                Page<CategoryResponseDTO> categoryDTOPage = categoryRepository.searchByName(cleanKey, pageable).map(category -> new CategoryResponseDTO(
                        category.getId(), category.getName()
                ));
                 yield new SearchResultResponseDTO(null, null, null, null, categoryDTOPage);
            }
            default -> new SearchResultResponseDTO(null, null, null, null, null);
        };
    }
}
