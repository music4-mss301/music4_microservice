package vn.edu.fpt.mss.searchadminservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import vn.edu.fpt.mss.searchadminservice.client.AiClient;
import vn.edu.fpt.mss.searchadminservice.client.ContentClient;
import vn.edu.fpt.mss.searchadminservice.config.IndexNames;
import vn.edu.fpt.mss.searchadminservice.dto.client.*;
import vn.edu.fpt.mss.searchadminservice.dto.response.ReindexStatus;
import vn.edu.fpt.mss.searchadminservice.es.EsClient;
import vn.edu.fpt.mss.searchadminservice.es.IndexInitializer;
import vn.edu.fpt.mss.searchadminservice.exception.BusinessException;
import vn.edu.fpt.mss.searchadminservice.service.IndexingService;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final EsClient es;
    private final AiClient aiClient;
    private final ContentClient contentClient;
    private final IndexInitializer indexInitializer;

    /** Reindex chạy nền trên 1 thread riêng để request admin trả về ngay (202). */
    private final ExecutorService reindexExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean reindexing = new AtomicBoolean(false);
    private volatile ReindexStatus lastStatus = new ReindexStatus(false, null, null, 0, null);

    @PreDestroy
    void shutdown() {
        reindexExecutor.shutdownNow();
    }

    private long countIndex(String index) {
        JsonNode res = es.search(index, Map.of("size", 0));
        return res.path("hits").path("total").path("value").asLong(0);
    }

    /** Khi khởi động: nếu index albums hoặc artists đang rỗng mà có tracks, tự động đồng bộ từ dữ liệu tracks đã có trong ES. */
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartupIfNeeded() {
        reindexExecutor.execute(() -> {
            try {
                long albumCount = countIndex(IndexNames.ALBUMS);
                long artistCount = countIndex(IndexNames.ARTISTS);
                long trackCount = countIndex(IndexNames.TRACKS);
                if (trackCount > 0 && (albumCount == 0 || artistCount <= 1)) {
                    log.info("Khởi động: phát hiện index albums/artists chưa đầy đủ (tracks={}, albums={}, artists={}), tự động tổng hợp từ music_tracks...",
                            trackCount, albumCount, artistCount);
                    rebuildAlbumsAndArtistsFromTracks();
                } else if (trackCount == 0) {
                    log.info("Khởi động: music_tracks đang rỗng, thử đồng bộ từ content-service...");
                    if (reindexing.compareAndSet(false, true)) {
                        lastStatus = new ReindexStatus(true, OffsetDateTime.now(), null, 0, null);
                        runReindex();
                    }
                }
            } catch (Exception e) {
                log.warn("Bỏ qua tự động đồng bộ ES lúc khởi động: {}", e.getMessage());
            }
        });
    }

    /** Tổng hợp albums và artists trực tiếp từ các document đang có trong music_tracks (không phụ thuộc mạng sang content-service). */
    private void rebuildAlbumsAndArtistsFromTracks() {
        indexInitializer.ensureIndices();
        JsonNode res = es.search(IndexNames.TRACKS, Map.of("size", 1000, "_source", Map.of("excludes", List.of("vector"))));
        Map<Integer, AlbumDoc> albumsMap = new LinkedHashMap<>();
        Map<Integer, Integer> albumTrackCounts = new HashMap<>();
        Map<Integer, ArtistDoc> artistsMap = new LinkedHashMap<>();
        Map<Integer, Integer> artistTrackCounts = new HashMap<>();
        Map<Integer, Set<Integer>> artistAlbums = new HashMap<>();

        for (JsonNode hit : res.path("hits").path("hits")) {
            JsonNode src = hit.path("_source");
            String img = src.path("img").isNull() ? null : src.path("img").asText(null);
            Integer albumId = src.path("albumId").isNumber() ? src.path("albumId").asInt() : null;
            String albumName = src.path("albumName").isNull() ? null : src.path("albumName").asText(null);
            JsonNode artistsNode = src.path("artists");

            Integer mainArtistId = null;
            String mainArtistName = null;
            if (artistsNode.isArray() && !artistsNode.isEmpty()) {
                JsonNode first = artistsNode.get(0);
                mainArtistId = first.path("id").isNumber() ? first.path("id").asInt() : null;
                mainArtistName = first.path("name").asText(null);
            }

            if (albumId != null && albumName != null && !albumName.isBlank()) {
                albumTrackCounts.merge(albumId, 1, Integer::sum);
                albumsMap.putIfAbsent(albumId, new AlbumDoc(albumId, albumName, img, null, 0, mainArtistId, mainArtistName));
            }

            if (artistsNode.isArray()) {
                for (JsonNode a : artistsNode) {
                    if (!a.path("id").isNumber()) continue;
                    int aId = a.path("id").asInt();
                    String aName = a.path("name").asText(null);
                    artistTrackCounts.merge(aId, 1, Integer::sum);
                    if (albumId != null) {
                        artistAlbums.computeIfAbsent(aId, k -> new HashSet<>()).add(albumId);
                    }
                    artistsMap.putIfAbsent(aId, new ArtistDoc(aId, null, aName, img, 0, 0));
                }
            }
        }

        for (AlbumDoc al : albumsMap.values()) {
            int count = albumTrackCounts.getOrDefault(al.albumId(), 0);
            indexAlbum(new AlbumDoc(al.albumId(), al.name(), al.img(), al.uploadDate(), count, al.artistId(), al.artistName()));
        }
        for (ArtistDoc ar : artistsMap.values()) {
            int tCount = artistTrackCounts.getOrDefault(ar.artistId(), 0);
            int aCount = artistAlbums.getOrDefault(ar.artistId(), Set.of()).size();
            indexArtist(new ArtistDoc(ar.artistId(), ar.userId(), ar.name(), ar.img(), tCount, aCount));
        }
        log.info("Đã tổng hợp và index {} albums, {} artists từ music_tracks", albumsMap.size(), artistsMap.size());
    }

    @Override
    public void indexTrack(TrackDoc t) {
        es.put(IndexNames.TRACKS, String.valueOf(t.trackId()), toTrackDocument(t));
    }

    @Override
    public void deleteTrack(Integer trackId) {
        es.delete(IndexNames.TRACKS, String.valueOf(trackId));
    }

    @Override
    public void indexAlbum(AlbumDoc a) {
        es.put(IndexNames.ALBUMS, String.valueOf(a.albumId()), a);
    }

    @Override
    public void deleteAlbum(Integer albumId) {
        es.delete(IndexNames.ALBUMS, String.valueOf(albumId));
    }

    @Override
    public void indexArtist(ArtistDoc a) {
        es.put(IndexNames.ARTISTS, String.valueOf(a.artistId()), a);
    }

    @Override
    public void deleteArtist(Integer artistId) {
        es.delete(IndexNames.ARTISTS, String.valueOf(artistId));
    }

    /** Đổi tên thể loại: cập nhật mọi bài có thể loại đó bằng update_by_query (painless script). */
    @Override
    public void renameCategory(Integer categoryId, String newName) {
        String script = """
                for (c in ctx._source.categories) { if (c.id == params.id) { c.name = params.name; } }
                ctx._source.categoryNames = ctx._source.categories.stream().map(c -> c.name).collect(Collectors.toList());
                ctx._source.needsEmbedding = true;
                """;
        es.updateByQuery(IndexNames.TRACKS, Map.of(
                "query", Map.of("term", Map.of("categories.id", categoryId)),
                "script", Map.of("source", script, "lang", "painless",
                        "params", Map.of("id", categoryId, "name", newName))));
    }

    @Override
    public void removeCategory(Integer categoryId) {
        log.info("Category {} bị xóa: content chỉ cho xóa thể loại không còn bài nên không cần cập nhật ES", categoryId);
    }

    /** Rule: chỉ 1 lần reindex chạy tại một thời điểm, gọi tiếp khi đang chạy => 409. */
    @Override
    public ReindexStatus startReindex() {
        if (!reindexing.compareAndSet(false, true)) {
            throw new BusinessException("Đang reindex, vui lòng đợi lần chạy hiện tại kết thúc");
        }
        lastStatus = new ReindexStatus(true, OffsetDateTime.now(), null, 0, null);
        reindexExecutor.execute(this::runReindex);
        return lastStatus;
    }

    private void runReindex() {
        int indexed = 0;
        String error = null;
        try {
            indexInitializer.ensureIndices();
            Map<Integer, AlbumDoc> albumsMap = new LinkedHashMap<>();
            Map<Integer, Integer> albumTrackCounts = new HashMap<>();
            Map<Integer, ArtistDoc> artistsMap = new LinkedHashMap<>();
            Map<Integer, Integer> artistTrackCounts = new HashMap<>();
            Map<Integer, Set<Integer>> artistAlbums = new HashMap<>();

            int page = 0;
            PageResult<TrackDoc> batch;
            do {
                batch = contentClient.export(page++, 100);
                for (TrackDoc t : batch.content()) {
                    indexTrack(t);
                    indexed++;

                    TrackDoc.Ref mainArtist = (t.artists() != null && !t.artists().isEmpty()) ? t.artists().getFirst() : null;
                    if (t.albumId() != null && t.albumName() != null) {
                        albumTrackCounts.merge(t.albumId(), 1, Integer::sum);
                        albumsMap.putIfAbsent(t.albumId(), new AlbumDoc(
                                t.albumId(), t.albumName(), t.img(), t.uploadDate(), 0,
                                mainArtist != null ? mainArtist.id() : null,
                                mainArtist != null ? mainArtist.name() : null));
                    }
                    if (t.artists() != null) {
                        for (TrackDoc.Ref a : t.artists()) {
                            if (a.id() == null) continue;
                            artistTrackCounts.merge(a.id(), 1, Integer::sum);
                            if (t.albumId() != null) {
                                artistAlbums.computeIfAbsent(a.id(), k -> new HashSet<>()).add(t.albumId());
                            }
                            artistsMap.putIfAbsent(a.id(), new ArtistDoc(a.id(), null, a.name(), t.img(), 0, 0));
                        }
                    }
                }
            } while (!batch.last());

            for (AlbumDoc al : albumsMap.values()) {
                int count = albumTrackCounts.getOrDefault(al.albumId(), 0);
                indexAlbum(new AlbumDoc(al.albumId(), al.name(), al.img(), al.uploadDate(), count, al.artistId(), al.artistName()));
            }
            for (ArtistDoc ar : artistsMap.values()) {
                int tCount = artistTrackCounts.getOrDefault(ar.artistId(), 0);
                int aCount = artistAlbums.getOrDefault(ar.artistId(), Set.of()).size();
                indexArtist(new ArtistDoc(ar.artistId(), ar.userId(), ar.name(), ar.img(), tCount, aCount));
            }
            log.info("Reindex xong {} bài, {} album, {} nghệ sĩ", indexed, albumsMap.size(), artistsMap.size());
        } catch (Exception e) {
            error = e.getMessage();
            log.error("Reindex thất bại sau {} bài: {}", indexed, error);
        } finally {
            lastStatus = new ReindexStatus(false, lastStatus.lastStartedAt(), OffsetDateTime.now(), indexed, error);
            reindexing.set(false);
        }
    }

    @Override
    public ReindexStatus status() {
        return lastStatus;
    }

    /** Embed bù các bài được index lúc AI-service đang lỗi. Chạy định kỳ. */
    @Override
    @Scheduled(fixedDelayString = "${app.search.embed-retry-ms:600000}", initialDelay = 60000)
    public int embedPending() {
        int done = 0;
        try {
            JsonNode res = es.search(IndexNames.TRACKS, Map.of("size", 50,
                    "query", Map.of("term", Map.of("needsEmbedding", true)),
                    "_source", Map.of("excludes", List.of("vector"))));
            for (JsonNode hit : res.path("hits").path("hits")) {
                JsonNode src = hit.path("_source");
                List<Float> vector = aiClient.embed(embeddingText(src.path("name").asText(),
                        src.path("artistNames"), src.path("categoryNames")));
                if (vector == null) {
                    break; // AI vẫn lỗi, để lần sau
                }
                es.partialUpdate(IndexNames.TRACKS, hit.path("_id").asText(),
                        Map.of("vector", vector, "needsEmbedding", false));
                done++;
            }
        } catch (Exception e) {
            log.debug("Bỏ qua embed bù: {}", e.getMessage());
        }
        return done;
    }

    private Map<String, Object> toTrackDocument(TrackDoc t) {
        List<String> artistNames = t.artists() == null ? List.of()
                : t.artists().stream().map(TrackDoc.Ref::name).toList();
        List<String> categoryNames = t.categories() == null ? List.of()
                : t.categories().stream().map(TrackDoc.Ref::name).toList();
        List<Float> vector = aiClient.embed(t.name() + ". " + String.join(", ", artistNames) + ". "
                + String.join(", ", categoryNames));
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("trackId", t.trackId());
        doc.put("name", t.name());
        doc.put("img", t.img());
        doc.put("duration", t.duration());
        doc.put("previewPath", t.previewPath());
        doc.put("uploadDate", t.uploadDate() == null ? null : t.uploadDate().toString());
        doc.put("viewCount", t.viewCount());
        doc.put("albumId", t.albumId());
        doc.put("albumName", t.albumName());
        doc.put("artists", t.artists() == null ? List.of() : t.artists());
        doc.put("artistNames", artistNames);
        doc.put("categories", t.categories() == null ? List.of() : t.categories());
        doc.put("categoryNames", categoryNames);
        doc.put("needsEmbedding", vector == null);
        if (vector != null) {
            doc.put("vector", vector);
        }
        return doc;
    }

    private String embeddingText(String name, JsonNode artists, JsonNode categories) {
        List<String> a = new ArrayList<>();
        artists.forEach(n -> a.add(n.asText()));
        List<String> c = new ArrayList<>();
        categories.forEach(n -> c.add(n.asText()));
        return name + ". " + String.join(", ", a) + ". " + String.join(", ", c);
    }
}
