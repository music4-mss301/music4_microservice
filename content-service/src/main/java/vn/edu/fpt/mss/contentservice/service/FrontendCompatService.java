package vn.edu.fpt.mss.contentservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.mss.contentservice.client.EngagementClient;
import vn.edu.fpt.mss.contentservice.dto.client.ArtistEngagementQuery;
import vn.edu.fpt.mss.contentservice.dto.client.ArtistEngagementResult;
import vn.edu.fpt.mss.contentservice.dto.response.*;
import vn.edu.fpt.mss.contentservice.entity.Artist;
import vn.edu.fpt.mss.contentservice.exception.ResourceNotFoundException;
import vn.edu.fpt.mss.contentservice.mapper.ContentMapper;
import vn.edu.fpt.mss.contentservice.repository.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Các API mà FE (viết cho monolith) đang gọi nhưng bản microservices ban đầu chưa có. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FrontendCompatService {

    private final TrackService trackService;
    private final AlbumService albumService;
    private final ArtistRepository artistRepository;
    private final TrackRepository trackRepository;
    private final TrackIdRepository trackIdRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryStatsRepository categoryStatsRepository;
    private final TrackByCategoryRepository trackByCategoryRepository;
    private final EngagementClient engagementClient;

    public List<TrackResponse> top5MostViewed() {
        return trackService.getTrending(5);
    }

    /** Cùng seed thì thứ tự giống hệt nhau, nên FE cuộn "xem thêm" qua nhiều trang vẫn không trùng bài. */
    public PageResponse<TrackResponse> randomTracks(int seed, int page, int size) {
        int s = Math.min(Math.max(size, 1), 50);
        int p = Math.max(page, 0);
        List<Integer> ids = new ArrayList<>(trackIdRepository.findAllIds());
        Collections.shuffle(ids, new Random(seed));
        int from = Math.min(p * s, ids.size());
        int to = Math.min(from + s, ids.size());
        return PageResponse.of(trackService.getByIds(ids.subList(from, to)), p, s, ids.size());
    }

    public long countArtists() {
        return artistRepository.countByIsDeletedFalse();
    }

    /** Điểm = người theo dõi + lượt thích bài hát. Hai số này nằm ở engagement-service nên gọi 1 lần cho cả danh sách. */
    public List<TopArtistResponse> top3Artists() {
        List<Artist> artists = artistRepository.findByIsDeletedFalseOrderByNameAsc();
        if (artists.isEmpty()) {
            return List.of();
        }
        List<ArtistEngagementQuery> queries = artists.stream()
                .map(a -> new ArtistEngagementQuery(a.getId(), trackRepository.findIdsByArtistId(a.getId()))).toList();
        Map<Integer, ArtistEngagementResult> stats = engagementClient.artistEngagement(queries).stream()
                .collect(Collectors.toMap(ArtistEngagementResult::artistId, Function.identity(), (a, b) -> a));
        return artists.stream()
                .map(a -> {
                    ArtistEngagementResult r = stats.get(a.getId());
                    return new TopArtistResponse(a.getId(), a.getName(), a.getImg(), a.getTrackTotal(),
                            r == null ? 0 : r.followers(), r == null ? 0 : r.favorites());
                })
                .sorted(Comparator.comparingLong((TopArtistResponse t) -> t.followers() + t.favorites()).reversed()
                        .thenComparing(Comparator.comparingInt(TopArtistResponse::trackTotal).reversed()))
                .limit(3).toList();
    }

    public List<CategoryStatResponse> categoryStats() {
        return categoryStatsRepository.stats().stream()
                .map(r -> new CategoryStatResponse((Integer) r[0], (String) r[1], ((Number) r[2]).longValue())).toList();
    }

    public PageResponse<TrackResponse> tracksOfCategory(Integer categoryId, int page, int size) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Không tìm thấy thể loại id=" + categoryId);
        }
        return PageResponse.of(trackByCategoryRepository.findByCategoryId(categoryId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))), ContentMapper::toTrackResponse);
    }

    public PageResponse<AlbumResponse> allAlbums(int page, int size) {
        return albumService.getAll(page, size);
    }
}
