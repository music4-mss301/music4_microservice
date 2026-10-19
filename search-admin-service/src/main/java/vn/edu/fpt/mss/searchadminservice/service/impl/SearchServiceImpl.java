package vn.edu.fpt.mss.searchadminservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import vn.edu.fpt.mss.searchadminservice.client.ContentClient;
import vn.edu.fpt.mss.searchadminservice.config.IndexNames;
import vn.edu.fpt.mss.searchadminservice.dto.client.PageResult;
import vn.edu.fpt.mss.searchadminservice.dto.client.TrackSummary;
import vn.edu.fpt.mss.searchadminservice.dto.request.RecommendQuery;
import vn.edu.fpt.mss.searchadminservice.dto.response.*;
import vn.edu.fpt.mss.searchadminservice.es.EsClient;
import vn.edu.fpt.mss.searchadminservice.exception.BadRequestException;
import vn.edu.fpt.mss.searchadminservice.mapper.SearchMapper;
import vn.edu.fpt.mss.searchadminservice.service.CurationService;
import vn.edu.fpt.mss.searchadminservice.service.SearchService;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private static final Set<String> TYPES = Set.of("all", "tracks", "albums", "artists");

    private final EsClient es;
    private final ContentClient contentClient;
    private final CurationService curationService;
    private final CircuitBreakerFactory<?, ?> cbFactory;

    /** Circuit breaker "elasticsearch": ES lỗi thì tìm bài hát bằng DB của content-service. */
    @Override
    public SearchResponse search(String q, String type, int page, int size) {
        if (!StringUtils.hasText(q) || q.trim().length() > 100) {
            throw new BadRequestException("Từ khóa phải từ 1 đến 100 ký tự");
        }
        String t = type == null ? "all" : type.toLowerCase();
        if (!TYPES.contains(t)) {
            throw new BadRequestException("type chỉ nhận all | tracks | albums | artists");
        }
        String query = q.trim();
        int s = Math.min(Math.max(size, 1), 50);
        int p = Math.max(page, 0);
        return cbFactory.create("elasticsearch").run(() -> searchEs(query, t, p, s), ex -> {
            log.warn("ES lỗi, fallback tìm trong DB: {}", ex.getMessage());
            return searchDb(query, p, s);
        });
    }

    @Override
    public List<String> suggest(String q) {
        if (!StringUtils.hasText(q)) {
            return List.of();
        }
        return cbFactory.create("elasticsearch").run(() -> {
            JsonNode res = es.search(IndexNames.TRACKS, Map.of("size", 5, "_source", List.of("name"),
                    "query", Map.of("match_phrase_prefix", Map.of("name", q.trim()))));
            List<String> names = new ArrayList<>();
            res.path("hits").path("hits").forEach(h -> names.add(h.path("_source").path("name").asText()));
            return names.stream().distinct().toList();
        }, ex -> List.of());
    }

    /**
     * kNN trên vector: lấy vector các bài user thích, tính trung bình, tìm bài gần nhất (cosine),
     * loại bài đã nghe/không thích. Không đủ vector thì trả rỗng để analytics dùng trending.
     */
    @Override
    public List<SuggestionResponse> recommend(RecommendQuery query) {
        if (query.likedTrackIds() == null || query.likedTrackIds().isEmpty()) {
            return List.of();
        }
        return cbFactory.create("elasticsearch").run(() -> {
            List<String> ids = query.likedTrackIds().stream().limit(50).map(String::valueOf).toList();
            JsonNode docs = es.mget(IndexNames.TRACKS, ids).path("docs");
            float[] sum = null;
            int count = 0;
            for (JsonNode d : docs) {
                JsonNode v = d.path("_source").path("vector");
                if (!v.isArray() || v.isEmpty()) {
                    continue;
                }
                if (sum == null) {
                    sum = new float[v.size()];
                }
                for (int i = 0; i < sum.length && i < v.size(); i++) {
                    sum[i] += (float) v.get(i).asDouble();
                }
                count++;
            }
            if (sum == null) {
                return List.<SuggestionResponse>of();
            }
            List<Float> avg = new ArrayList<>(sum.length);
            for (float f : sum) {
                avg.add(f / count);
            }
            Set<Integer> exclude = new HashSet<>();
            if (query.excludeTrackIds() != null) {
                exclude.addAll(query.excludeTrackIds());
            }
            if (query.dislikedTrackIds() != null) {
                exclude.addAll(query.dislikedTrackIds());
            }
            Map<String, Object> knn = new HashMap<>();
            knn.put("field", "vector");
            knn.put("query_vector", avg);
            knn.put("k", query.size());
            knn.put("num_candidates", Math.max(100, query.size() * 10));
            if (!exclude.isEmpty()) {
                knn.put("filter", Map.of("bool", Map.of("must_not", List.of(Map.of("terms", Map.of("trackId", exclude))))));
            }
            JsonNode res = es.search(IndexNames.TRACKS, Map.of("knn", knn, "size", query.size(),
                    "_source", Map.of("excludes", List.of("vector"))));
            List<SuggestionResponse> out = new ArrayList<>();
            res.path("hits").path("hits").forEach(h -> out.add(SearchMapper.toSuggestion(h)));
            return out;
        }, ex -> {
            log.warn("kNN lỗi: {}", ex.getMessage());
            return List.of();
        });
    }

    private SearchResponse searchEs(String q, String type, int page, int size) {
        HitPage<TrackHit> tracks = HitPage.empty(page, size);
        HitPage<AlbumHit> albums = HitPage.empty(page, size);
        HitPage<ArtistHit> artists = HitPage.empty(page, size);
        if (type.equals("all") || type.equals("tracks")) {
            JsonNode res = es.search(IndexNames.TRACKS, body(q, page, size,
                    List.of("name^3", "artistNames^2", "albumName", "categoryNames")));
            List<TrackHit> hits = new ArrayList<>();
            res.path("hits").path("hits").forEach(h -> hits.add(SearchMapper.toTrackHit(h, false)));
            tracks = new HitPage<>(applyCuration(q, page, hits), page, size, total(res));
        }
        if (type.equals("all") || type.equals("albums")) {
            JsonNode res = es.search(IndexNames.ALBUMS, body(q, page, size, List.of("name^2", "artistName")));
            List<AlbumHit> hits = new ArrayList<>();
            res.path("hits").path("hits").forEach(h -> hits.add(SearchMapper.toAlbumHit(h)));
            albums = new HitPage<>(hits, page, size, total(res));
        }
        if (type.equals("all") || type.equals("artists")) {
            JsonNode res = es.search(IndexNames.ARTISTS, body(q, page, size, List.of("name")));
            List<ArtistHit> hits = new ArrayList<>();
            res.path("hits").path("hits").forEach(h -> hits.add(SearchMapper.toArtistHit(h)));
            artists = new HitPage<>(hits, page, size, total(res));
        }
        return new SearchResponse(q, tracks, albums, artists, "elasticsearch");
    }

    /** Bài được ADMIN ghim cho đúng từ khóa này hiện lên đầu trang 1. */
    private List<TrackHit> applyCuration(String q, int page, List<TrackHit> hits) {
        if (page != 0) {
            return hits;
        }
        return curationService.findByKeyword(q).map(c -> {
            List<String> ids = c.trackIds().stream().map(String::valueOf).toList();
            List<TrackHit> pinned = new ArrayList<>();
            es.mget(IndexNames.TRACKS, ids).path("docs").forEach(d -> {
                if (d.path("found").asBoolean(false)) {
                    pinned.add(SearchMapper.toTrackHit(d, true));
                }
            });
            Set<Integer> pinnedIds = new HashSet<>(c.trackIds());
            hits.stream().filter(h -> !pinnedIds.contains(h.trackId())).forEach(pinned::add);
            return pinned;
        }).orElse(hits);
    }

    private SearchResponse searchDb(String q, int page, int size) {
        PageResult<TrackSummary> res = contentClient.search(q, page, size);
        List<TrackHit> hits = res.content().stream().map(SearchMapper::fromDb).toList();
        return new SearchResponse(q, new HitPage<>(hits, page, size, res.totalElements()),
                HitPage.empty(page, size), HitPage.empty(page, size), "database");
    }

    private Map<String, Object> body(String q, int page, int size, List<String> fields) {
        return Map.of("from", page * size, "size", size,
                "_source", Map.of("excludes", List.of("vector")),
                "query", Map.of("multi_match", Map.of("query", q, "fields", fields, "fuzziness", "AUTO")));
    }

    private long total(JsonNode res) {
        return res.path("hits").path("total").path("value").asLong(0);
    }
}
