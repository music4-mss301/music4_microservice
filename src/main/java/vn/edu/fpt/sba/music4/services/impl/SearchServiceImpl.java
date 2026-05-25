package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.fpt.sba.music4.dtos.response.SearchResultResponseDTO;
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
            return new SearchResultResponseDTO(
                    trackRepository.searchByName(cleanKey, pageable),
                    albumRepository.searchByName(cleanKey, pageable),
                    artistRepository.searchByName(cleanKey, pageable),
                    playlistRepository.searchByName(cleanKey, pageable),
                    categoryRepository.searchByName(cleanKey, pageable)
            );
        }

        // 2. TÌM KIẾM CHI TIẾT THEO TYPE
        return switch (type.toLowerCase()) {
            case "track" -> new SearchResultResponseDTO(trackRepository.searchByName(cleanKey, pageable), null, null, null, null);
            case "artist" -> new SearchResultResponseDTO(null, null, artistRepository.searchByName(cleanKey, pageable), null, null);
            case "album" -> new SearchResultResponseDTO(null, albumRepository.searchByName(cleanKey, pageable), null, null, null);
            case "playlist" -> new SearchResultResponseDTO(null, null, null, playlistRepository.searchByName(cleanKey, pageable), null);
            case "category" -> new SearchResultResponseDTO(null, null, null, null, categoryRepository.searchByName(cleanKey, pageable));
            default -> new SearchResultResponseDTO(null, null, null, null, null);
        };
    }
}
