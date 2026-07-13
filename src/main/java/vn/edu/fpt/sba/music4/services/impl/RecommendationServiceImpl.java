package vn.edu.fpt.sba.music4.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import vn.edu.fpt.sba.music4.dtos.response.TrackArtistInfo;
import vn.edu.fpt.sba.music4.dtos.response.TrackSuggestResponse;
import vn.edu.fpt.sba.music4.entities.Track;
import vn.edu.fpt.sba.music4.repositories.FavoriteRepository;
import vn.edu.fpt.sba.music4.repositories.HistoryRepository;
import vn.edu.fpt.sba.music4.repositories.RatingRepository;
import vn.edu.fpt.sba.music4.repositories.TrackRepository;
import vn.edu.fpt.sba.music4.services.IRecommendationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements IRecommendationService {

    private final TrackRepository trackRepository;
    private final RatingRepository ratingRepository;
    private final FavoriteRepository favoriteRepository;
    private final HistoryRepository historyRepository;

    private final RestTemplate restTemplate;

    @Value("${app.python.embed.url}")
    private String pythonEmbedUrl;

    @Value("${app.elasticsearch.url}")
    private String esIndexUrl;

    @Value("${app.es.search.url}")
    private String esSearchUrl;

    // === HÀM LẤY GỢI Ý BÀI HÁT ===
    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<TrackSuggestResponse> getRecommendations(Integer userId) {
        log.info("[DEBUG] Bắt đầu lấy gợi ý cho User ID: {}", userId);

        // Dành cho Guest (Chưa đăng nhập): Chỉ trả về Trending Tracks
        if (userId == null) {
            log.warn("[DEBUG] User ID null -> Chuyển sang Fallback (Top Trending)");
            return getTrendingFallback();
        }

        // --- BƯỚC 1: LẤY LỊCH SỬ NGHE NHẠC CỦA USER VÀ TẠO THÀNH CHUỖI 'TEXT' ---
        Set<Integer> likedTrackIds = new HashSet<>();
        historyRepository.findByUserId(userId).forEach(h -> likedTrackIds.add(h.getTrack().getId()));
        favoriteRepository.findByUserId(userId).forEach(f -> likedTrackIds.add(f.getTrack().getId()));
        ratingRepository.findByUserId(userId).stream()
                .filter(r -> r.getRatingValue() >= 4).forEach(r -> likedTrackIds.add(r.getTrack().getId()));

        log.info("[DEBUG] Số lượng bài hát user {} đã tương tác: {}", userId, likedTrackIds.size());

        if (likedTrackIds.isEmpty()) {
            log.warn("[DEBUG] Dữ liệu lịch sử trống! Không có base để AI phân tích -> Chuyển sang Fallback");
            return getTrendingFallback();
        }

        List<Track> likedTracks = trackRepository.findAllById(likedTrackIds);
        StringBuilder tasteProfileBuilder = new StringBuilder();
        for (Track t : likedTracks) {
            tasteProfileBuilder.append(t.getName()).append(" ");
            t.getTrackCategories().forEach(tc -> tasteProfileBuilder.append(tc.getCategory().getName()).append(" "));
            t.getTrackArtists().forEach(ta -> tasteProfileBuilder.append(ta.getArtist().getName()).append(" "));
        }

        String userTasteText = tasteProfileBuilder.toString().trim();
        List<Double> embedding = null;

        // --- BƯỚC 2: GỌI AI PYTHON ĐỂ LẤY VECTOR (NATIVE HTTP/1.1) ---
        try {
            log.info("Đang gọi AI Service để lấy Vector cho User ID: {}", userId);

            // 1. Dọn dẹp text an toàn tuyệt đối
            String safeText = userTasteText.replace("\"", "").replace("\n", " ").replace("\r", " ").trim();
            String rawJson = "{\"text\": \"" + safeText + "\"}";

            // 2. KHỞI TẠO CLIENT VÀ ÉP DÙNG HTTP/1.1 (Chìa khóa ở đây!)
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1) // <--- Bắt buộc Uvicorn phải hiểu
                    .build();

            // 3. Đóng gói Request tĩnh
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(pythonEmbedUrl.trim()))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", "fpt-music4-project-secret-key-2026")
                    .POST(HttpRequest.BodyPublishers.ofString(rawJson, StandardCharsets.UTF_8))
                    .build();

            // 4. Gửi đi và nhận kết quả
            HttpResponse<String> pythonResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 5. Kiểm tra mã trạng thái
            if (pythonResponse.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> responseMap = mapper.readValue(pythonResponse.body(), Map.class);

                if (responseMap.containsKey("vector")) {
                    embedding = (List<Double>) responseMap.get("vector");
                    log.info("[DEBUG] 🎉 Đã nhận thành công Vector từ Python AI!");
                }
            } else {
                log.error("AI Service từ chối với status {}: {}", pythonResponse.statusCode(), pythonResponse.body());
                embedding = null;
            }
        } catch (Exception e) {
            log.error("Lỗi mạng khi gọi AI Service: {}", e.getMessage(), e);
            embedding = null;
        }

        // --- BƯỚC 3: XỬ LÝ FALLBACK ---
        if (embedding == null || embedding.isEmpty()) {
            // Nếu AI sập -> Trả về nhạc Trending
            return getTrendingFallback();
        }

        // --- BƯỚC 4: ĐEM VECTOR SANG ELASTICSEARCH K-NN SEARCH (CÓ TRY-CATCH RIÊNG BẢO VỆ) ---
        try {
            Map<String, Object> knnQuery = new HashMap<>();
            Map<String, Object> knnParams = new HashMap<>();
            knnParams.put("field", "vector_data");
            knnParams.put("query_vector", embedding);
            knnParams.put("k", 15);
            knnParams.put("num_candidates", 100);
            knnQuery.put("knn", knnParams);
            knnQuery.put("_source", Collections.singletonList("track_id"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> esEntity = new HttpEntity<>(knnQuery, headers);

            ResponseEntity<Map> esResponse = restTemplate.exchange(esSearchUrl.trim(), HttpMethod.POST, esEntity, Map.class);

            // BÓC TÁCH KẾT QUẢ TỪ ES
            Map<String, Object> hitsObj = (Map<String, Object>) esResponse.getBody().get("hits");
            List<Map<String, Object>> hitsList = (List<Map<String, Object>>) hitsObj.get("hits");

            List<Integer> recommendedTrackIds = new ArrayList<>();
            Map<Integer, Double> scoreMap = new HashMap<>();

            for (Map<String, Object> hit : hitsList) {
                Map<String, Object> source = (Map<String, Object>) hit.get("_source");
                Integer trackId = (Integer) source.get("track_id");

                // Lọc bỏ bài đã nghe
                if (!likedTrackIds.contains(trackId)) {
                    Double score = (Double) hit.get("_score");
                    recommendedTrackIds.add(trackId);
                    scoreMap.put(trackId, score);
                }
            }

            // Map với SQL Server và trả về
            List<Track> finalTracks = trackRepository.findAllById(recommendedTrackIds);
            return finalTracks.stream()
                    .map(track -> {
                        List<TrackArtistInfo> artistInfos = track.getTrackArtists().stream()
                                .map(ta -> new TrackArtistInfo(
                                        ta.getArtist().getId(),
                                        ta.getArtist().getName(),
                                        ta.getRole()
                                )).toList();

                        return new TrackSuggestResponse(
                                track.getId(), track.getName(), track.getImg(), track.getDuration(),
                                track.getPreviewPath(), track.getViewCount(),
                                Math.round(scoreMap.getOrDefault(track.getId(), 0.0) * 100.0) / 100.0,
                                artistInfos
                        );
                    })
                    .sorted((t1, t2) -> Double.compare(t2.matchScore(), t1.matchScore()))
                    .limit(10)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Lỗi khi tìm kiếm trên Elasticsearch: {}", e.getMessage());
            return getTrendingFallback();
        }
    }

    private List<TrackSuggestResponse> getTrendingFallback() {

        log.warn("Hệ thống AI không phản hồi! Kích hoạt chế độ Fallback: Trả về Top 10 Trending...");

        return trackRepository.findTopTrendingTracks(PageRequest.of(0, 10))
                .stream()
                .map(track -> {
                    // Map danh sách nghệ sĩ
                    List<TrackArtistInfo> artistInfos = track.getTrackArtists().stream()
                            .map(ta -> new TrackArtistInfo(
                                    ta.getArtist().getId(),
                                    ta.getArtist().getName(),
                                    ta.getRole()
                            )).toList();

                    return new TrackSuggestResponse(
                            track.getId(),
                            track.getName(),
                            track.getImg(),
                            track.getDuration(),
                            track.getPreviewPath(),
                            track.getViewCount(),
                            null,
                            artistInfos
                    );
                })
                .collect(Collectors.toList());
    }

    // === HÀM ĐỒNG BỘ DỮ LIỆU TỪ SQL LÊN ELASTICSEARCH ===
    @Async
    @Transactional
    @Override
    @SuppressWarnings("unchecked")
    public void syncTracksToElasticsearch() {
        List<Track> allTracks = trackRepository.findAll();
        log.info("Bắt đầu đồng bộ {} bài hát lên Elasticsearch...", allTracks.size());

        for (Track track : allTracks) {
            try {
                // Gom Text mô tả bài hát
                StringBuilder textBuilder = new StringBuilder(track.getName() + " ");
                track.getTrackCategories().forEach(tc -> textBuilder.append(tc.getCategory().getName()).append(" "));
                track.getTrackArtists().forEach(ta -> textBuilder.append(ta.getArtist().getName()).append(" "));

                // Python dịch sang Vector
                Map<String, String> pythonRequest = new HashMap<>();
                pythonRequest.put("text", textBuilder.toString());
                HttpHeaders pythonHeaders = new HttpHeaders();
                pythonHeaders.setContentType(MediaType.APPLICATION_JSON);
                pythonHeaders.set("X-API-Key", "fpt-music4-project-secret-key-2026");
                HttpEntity<Map<String, String>> pythonEntity = new HttpEntity<>(pythonRequest, pythonHeaders);
                Map<String, Object> pythonResponse = restTemplate.postForObject(pythonEmbedUrl.trim(), pythonEntity, Map.class);
                List<Double> vector = (List<Double>) pythonResponse.get("vector");

                // Đẩy vào Elasticsearch lưu trữ
                Map<String, Object> esDoc = new HashMap<>();
                esDoc.put("track_id", track.getId());
                esDoc.put("vector_data", vector);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(esDoc, headers);

                restTemplate.exchange(esIndexUrl.trim() + track.getId(), HttpMethod.PUT, entity, Map.class);
                log.info("Đã đồng bộ thành công bài hát ID: {}", track.getId());

            } catch (Exception e) {
                log.error("Lỗi đồng bộ bài hát ID {}: {}", track.getId(), e.getMessage());
            }
        }
        log.info("Hoàn tất quá trình đồng bộ!");
    }
}