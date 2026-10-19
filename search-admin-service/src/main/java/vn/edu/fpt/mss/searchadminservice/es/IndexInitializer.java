package vn.edu.fpt.mss.searchadminservice.es;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import vn.edu.fpt.mss.searchadminservice.config.IndexNames;
import vn.edu.fpt.mss.searchadminservice.config.SearchProperties;

import java.util.Map;

/** Tạo index + mapping khi khởi động nếu chưa có. ES chưa sẵn sàng thì chỉ log (search sẽ dùng fallback DB). */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexInitializer implements ApplicationRunner {

    private final EsClient es;
    private final SearchProperties props;

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureIndices();
        } catch (Exception e) {
            log.error("Chưa tạo được index Elasticsearch ({}). Search sẽ fallback về content-service.", e.getMessage());
        }
    }

    public void ensureIndices() {
        createIfMissing(IndexNames.TRACKS, tracksMapping());
        createIfMissing(IndexNames.ALBUMS, Map.of("mappings", Map.of("properties", Map.of(
                "albumId", Map.of("type", "integer"), "name", text(), "artistName", text(),
                "artistId", Map.of("type", "integer"), "uploadDate", Map.of("type", "date")))));
        createIfMissing(IndexNames.ARTISTS, Map.of("mappings", Map.of("properties", Map.of(
                "artistId", Map.of("type", "integer"), "name", text()))));
        createIfMissing(IndexNames.CURATIONS, Map.of("mappings", Map.of("properties", Map.of(
                "keyword", Map.of("type", "keyword"), "trackIds", Map.of("type", "integer"),
                "note", Map.of("type", "text"), "createdAt", Map.of("type", "date")))));
    }

    public Map<String, Object> tracksMapping() {
        return Map.of("mappings", Map.of("properties", Map.of(
                "trackId", Map.of("type", "integer"),
                "name", text(),
                "artistNames", text(),
                "albumName", text(),
                "categoryNames", text(),
                "viewCount", Map.of("type", "long"),
                "uploadDate", Map.of("type", "date"),
                "needsEmbedding", Map.of("type", "boolean"),
                "vector", Map.of("type", "dense_vector", "dims", props.vectorDims(),
                        "index", true, "similarity", "cosine"))));
    }

    private void createIfMissing(String index, Map<String, Object> body) {
        if (!es.indexExists(index)) {
            es.createIndex(index, body);
            log.info("Đã tạo index {}", index);
        }
    }

    private static Map<String, Object> text() {
        return Map.of("type", "text", "fields", Map.of("keyword", Map.of("type", "keyword", "ignore_above", 256)));
    }
}
