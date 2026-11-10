package vn.edu.fpt.mss.searchadminservice.dto.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression: content-service trả PageResponse có cả "page": { ... } (đối tượng lồng cho FE cũ).
 * PageResult từng khai báo "int page" nên Feign báo DecodeException và reindex thất bại sau 0 bài.
 */
class PageResultTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void decodesContentServicePageResponseWithNestedPageObject() throws Exception {
        String json = """
                {"content":[{"trackId":7,"name":"Intro","img":"covers/a.jpg","duration":58,
                  "filePath":"tracks/a.mp3","previewPath":"tracks/a.mp3","uploadDate":"2026-11-09",
                  "viewCount":3,"albumId":null,"albumName":null,
                  "artists":[{"id":11,"name":"Obito"}],"categories":[]}],
                 "page":{"size":100,"number":0,"totalElements":1,"totalPages":1},
                 "number":0,"size":100,"totalElements":1,"totalPages":1,"last":true}
                """;

        PageResult<TrackDoc> result = mapper.readValue(json, new TypeReference<>() {
        });

        assertEquals(1, result.content().size());
        assertEquals(7, result.content().get(0).trackId());
        assertEquals("Obito", result.content().get(0).artists().get(0).name());
        assertEquals(0, result.number());
        assertTrue(result.last());
    }

    @Test
    void notLastPageIsReadCorrectly() throws Exception {
        String json = "{\"content\":[],\"page\":{\"size\":100,\"number\":2,\"totalElements\":300,\"totalPages\":3},"
                + "\"number\":2,\"size\":100,\"totalElements\":300,\"totalPages\":3,\"last\":false}";

        PageResult<TrackDoc> result = mapper.readValue(json, new TypeReference<>() {
        });

        assertEquals(2, result.number());
        assertFalse(result.last());
    }
}
