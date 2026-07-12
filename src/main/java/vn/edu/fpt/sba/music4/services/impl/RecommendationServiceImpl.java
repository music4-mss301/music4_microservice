package vn.edu.fpt.sba.music4.services.impl;

import lombok.RequiredArgsConstructor;
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

    private final String PYTHON_EMBED_URL = "http://127.0.0.1:8000/api/ai/embed";
    private final String ES_INDEX_URL = "http://127.0.0.1:9200/music_tracks/_doc/";
    private final String ES_SEARCH_URL = "http://127.0.0.1:9200/music_tracks/_knn_search";

    // === HÀM LẤY GỢI Ý BÀI HÁT ===
    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<TrackSuggestResponse> getRecommendations(Integer userId) {
        // Dành cho Guest (Chưa đăng nhập): Chỉ trả về Trending Tracks
        if (userId == null) {
            return getTrendingFallback();
        }

        try {
            // LẤY GU ÂM NHẠC CỦA USER
            Set<Integer> likedTrackIds = new HashSet<>();
            historyRepository.findByUserId(userId).forEach(h -> likedTrackIds.add(h.getTrack().getId()));
            favoriteRepository.findByUserId(userId).forEach(f -> likedTrackIds.add(f.getTrack().getId()));
            ratingRepository.findByUserId(userId).stream()
                    .filter(r -> r.getRatingValue() >= 4).forEach(r -> likedTrackIds.add(r.getTrack().getId()));

            if (likedTrackIds.isEmpty()) return getTrendingFallback();

            List<Track> likedTracks = trackRepository.findAllById(likedTrackIds);

            // Gom text của tất cả bài user thích để tạo thành 1 đoạn văn "Mô tả Gu"
            StringBuilder tasteProfileBuilder = new StringBuilder();
            for (Track t : likedTracks) {
                tasteProfileBuilder.append(t.getName()).append(" ");
                t.getTrackCategories().forEach(tc -> tasteProfileBuilder.append(tc.getCategory().getName()).append(" "));
                t.getTrackArtists().forEach(ta -> tasteProfileBuilder.append(ta.getArtist().getName()).append(" "));
            }

            // PYTHON DỊCH "ĐOẠN VĂN GU" THÀNH VECTOR
            Map<String, String> pythonRequest = new HashMap<>();
            pythonRequest.put("text", tasteProfileBuilder.toString());


            HttpHeaders pythonHeaders = new HttpHeaders();
            pythonHeaders.setContentType(MediaType.APPLICATION_JSON);
            pythonHeaders.set("X-API-Key", "fpt-music4-project-secret-key-2026");
            HttpEntity<Map<String, String>> pythonEntity = new HttpEntity<>(pythonRequest, pythonHeaders);
            Map<String, Object> pythonResponse = restTemplate.postForObject(PYTHON_EMBED_URL, pythonEntity, Map.class);
            List<Double> userTasteVector = (List<Double>) pythonResponse.get("vector");

            // ĐEM VECTOR ĐÓ SANG ELASTICSEARCH ĐỂ K-NN SEARCH
            Map<String, Object> knnQuery = new HashMap<>();
            Map<String, Object> knnParams = new HashMap<>();
            knnParams.put("field", "vector_data");
            knnParams.put("query_vector", userTasteVector);
            knnParams.put("k", 15);
            knnParams.put("num_candidates", 100);
            knnQuery.put("knn", knnParams);
            knnQuery.put("_source", Collections.singletonList("track_id"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> esEntity = new HttpEntity<>(knnQuery, headers);

            ResponseEntity<Map> esResponse = restTemplate.exchange(ES_SEARCH_URL, HttpMethod.POST, esEntity, Map.class);

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
            log.error("Lỗi hệ thống AI: {}", e.getMessage());
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
                Map<String, Object> pythonResponse = restTemplate.postForObject(PYTHON_EMBED_URL, pythonEntity, Map.class);
                List<Double> vector = (List<Double>) pythonResponse.get("vector");

                // Đẩy vào Elasticsearch lưu trữ
                Map<String, Object> esDoc = new HashMap<>();
                esDoc.put("track_id", track.getId());
                esDoc.put("vector_data", vector);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(esDoc, headers);

                restTemplate.exchange(ES_INDEX_URL + track.getId(), HttpMethod.PUT, entity, Map.class);
                log.info("Đã đồng bộ thành công bài hát ID: {}", track.getId());

            } catch (Exception e) {
                log.error("Lỗi đồng bộ bài hát ID {}: {}", track.getId(), e.getMessage());
            }
        }
        log.info("Hoàn tất quá trình đồng bộ!");
    }
}