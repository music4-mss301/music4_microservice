package vn.edu.fpt.mss.searchadminservice.es;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import vn.edu.fpt.mss.searchadminservice.config.SearchProperties;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Wrapper mỏng quanh REST API của Elasticsearch 8 (dùng JSON thuần, không phụ thuộc phiên bản client). */
@Component
public class EsClient {

    private final RestClient rest;

    public EsClient(SearchProperties props, RestClient.Builder builder) {
        RestClient.Builder b = builder.baseUrl(props.esUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (StringUtils.hasText(props.esUsername())) {
            String token = Base64.getEncoder().encodeToString(
                    (props.esUsername() + ":" + props.esPassword()).getBytes(StandardCharsets.UTF_8));
            b.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + token);
        }
        this.rest = b.build();
    }

    public boolean indexExists(String index) {
        try {
            rest.head().uri("/{index}", index).retrieve().toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }

    public void createIndex(String index, Map<String, Object> body) {
        rest.put().uri("/{index}", index).body(body).retrieve().toBodilessEntity();
    }

    public void deleteIndex(String index) {
        try {
            rest.delete().uri("/{index}", index).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ignored) {
            // không có thì thôi
        }
    }

    public void put(String index, String id, Object doc) {
        rest.put().uri("/{index}/_doc/{id}?refresh=wait_for", index, id).body(doc).retrieve().toBodilessEntity();
    }

    public void partialUpdate(String index, String id, Map<String, Object> fields) {
        rest.post().uri("/{index}/_update/{id}", index, id).body(Map.of("doc", fields)).retrieve().toBodilessEntity();
    }

    public void delete(String index, String id) {
        try {
            rest.delete().uri("/{index}/_doc/{id}?refresh=wait_for", index, id).retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ignored) {
            // idempotent: xóa document không tồn tại vẫn coi là thành công
        }
    }

    public Optional<JsonNode> get(String index, String id) {
        try {
            JsonNode node = rest.get().uri("/{index}/_doc/{id}", index, id).retrieve().body(JsonNode.class);
            return Optional.ofNullable(node).map(n -> n.get("_source"));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    public JsonNode mget(String index, List<String> ids) {
        return rest.post().uri("/{index}/_mget", index).body(Map.of("ids", ids)).retrieve().body(JsonNode.class);
    }

    public JsonNode search(String index, Map<String, Object> body) {
        return rest.post().uri("/{index}/_search", index).body(body).retrieve().body(JsonNode.class);
    }

    public void updateByQuery(String index, Map<String, Object> body) {
        rest.post().uri("/{index}/_update_by_query?conflicts=proceed&refresh=true", index).body(body)
                .retrieve().toBodilessEntity();
    }

    public void deleteByQuery(String index, Map<String, Object> body) {
        rest.post().uri("/{index}/_delete_by_query?conflicts=proceed&refresh=true", index).body(body)
                .retrieve().toBodilessEntity();
    }
}
