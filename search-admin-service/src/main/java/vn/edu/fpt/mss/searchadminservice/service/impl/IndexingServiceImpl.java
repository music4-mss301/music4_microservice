package vn.edu.fpt.mss.searchadminservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            int page = 0;
            PageResult<TrackDoc> batch;
            do {
                batch = contentClient.export(page++, 100);
                for (TrackDoc t : batch.content()) {
                    indexTrack(t);
                    indexed++;
                }
            } while (!batch.last());
            log.info("Reindex xong {} bài", indexed);
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
