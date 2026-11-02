package vn.edu.fpt.mss.searchadminservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.fpt.mss.searchadminservice.config.IndexNames;
import vn.edu.fpt.mss.searchadminservice.dto.request.CurationRequest;
import vn.edu.fpt.mss.searchadminservice.dto.response.CurationResponse;
import vn.edu.fpt.mss.searchadminservice.es.EsClient;
import vn.edu.fpt.mss.searchadminservice.exception.BusinessException;
import vn.edu.fpt.mss.searchadminservice.exception.ResourceNotFoundException;
import vn.edu.fpt.mss.searchadminservice.service.CurationService;

import java.time.OffsetDateTime;
import java.util.*;

/** CRUD "Curation" lưu trong ES index search_curations (kho dữ liệu riêng của search-admin). */
@Service
@RequiredArgsConstructor
public class CurationServiceImpl implements CurationService {

    private final EsClient es;
    private final ObjectMapper objectMapper;

    @Override
    public List<CurationResponse> getAll() {
        JsonNode res = es.search(IndexNames.CURATIONS, Map.of("size", 200, "query", Map.of("match_all", Map.of()),
                "sort", List.of(Map.of("createdAt", Map.of("order", "desc")))));
        List<CurationResponse> list = new ArrayList<>();
        res.path("hits").path("hits").forEach(h -> list.add(toResponse(h.path("_id").asText(), h.path("_source"))));
        return list;
    }

    @Override
    public CurationResponse get(String id) {
        return es.get(IndexNames.CURATIONS, id).map(src -> toResponse(id, src))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy curation id=" + id));
    }

    /** Rule: mỗi từ khóa chỉ có 1 curation (không phân biệt hoa thường), danh sách bài không trùng. */
    @Override
    public CurationResponse create(CurationRequest req) {
        String keyword = normalize(req.keyword());
        if (findByKeyword(keyword).isPresent()) {
            throw new BusinessException("Từ khóa '" + keyword + "' đã có curation");
        }
        String id = UUID.randomUUID().toString();
        save(id, keyword, req, OffsetDateTime.now());
        return get(id);
    }

    @Override
    public CurationResponse update(String id, CurationRequest req) {
        CurationResponse existing = get(id);
        String keyword = normalize(req.keyword());
        findByKeyword(keyword).filter(c -> !c.id().equals(id)).ifPresent(c -> {
            throw new BusinessException("Từ khóa '" + keyword + "' đã có curation khác");
        });
        save(id, keyword, req, existing.createdAt());
        return get(id);
    }

    @Override
    public void delete(String id) {
        get(id);
        es.delete(IndexNames.CURATIONS, id);
    }

    @Override
    public Optional<CurationResponse> findByKeyword(String keyword) {
        JsonNode res = es.search(IndexNames.CURATIONS, Map.of("size", 1,
                "query", Map.of("term", Map.of("keyword", normalize(keyword)))));
        JsonNode hit = res.path("hits").path("hits").path(0);
        return hit.isMissingNode() ? Optional.empty()
                : Optional.of(toResponse(hit.path("_id").asText(), hit.path("_source")));
    }

    private void save(String id, String keyword, CurationRequest req, OffsetDateTime createdAt) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("keyword", keyword);
        doc.put("trackIds", new ArrayList<>(new LinkedHashSet<>(req.trackIds())));
        doc.put("note", req.note());
        doc.put("createdAt", createdAt.toString());
        es.put(IndexNames.CURATIONS, id, doc);
    }

    private CurationResponse toResponse(String id, JsonNode src) {
        List<Integer> trackIds = new ArrayList<>();
        src.path("trackIds").forEach(n -> trackIds.add(n.asInt()));
        String created = src.path("createdAt").asText(null);
        return new CurationResponse(id, src.path("keyword").asText(), trackIds, src.path("note").asText(null),
                created == null ? null : OffsetDateTime.parse(created));
    }

    private static String normalize(String keyword) {
        return keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
    }
}
