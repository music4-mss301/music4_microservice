package vn.edu.fpt.mss.searchadminservice.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import vn.edu.fpt.mss.searchadminservice.client.ContentClient;
import vn.edu.fpt.mss.searchadminservice.dto.client.PageResult;
import vn.edu.fpt.mss.searchadminservice.dto.client.TrackSummary;
import vn.edu.fpt.mss.searchadminservice.dto.response.SearchResponse;
import vn.edu.fpt.mss.searchadminservice.es.EsClient;
import vn.edu.fpt.mss.searchadminservice.exception.BadRequestException;
import vn.edu.fpt.mss.searchadminservice.service.CurationService;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock
    private EsClient es;

    @Mock
    private ContentClient contentClient;

    @Mock
    private CurationService curationService;

    @Mock
    private CircuitBreakerFactory<?, ?> cbFactory;

    @InjectMocks
    private SearchServiceImpl searchService;

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t\n"})
    @DisplayName("Validation: Ném BadRequestException (400) khi từ khóa tìm kiếm rỗng hoặc chỉ có khoảng trắng")
    void search_whenQueryIsBlank_throwsBadRequestException(String blankQuery) {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> searchService.search(blankQuery, "all", 0, 20));
        assertTrue(ex.getMessage().contains("từ 1 đến 100 ký tự"));
    }

    @Test
    @DisplayName("Validation: Ném BadRequestException (400) khi từ khóa tìm kiếm vượt quá 100 ký tự")
    void search_whenQueryExceeds100Chars_throwsBadRequestException() {
        String longQuery = "a".repeat(101);
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> searchService.search(longQuery, "all", 0, 20));
        assertTrue(ex.getMessage().contains("từ 1 đến 100 ký tự"));
    }

    @Test
    @DisplayName("Validation: Ném BadRequestException (400) khi type không hợp lệ")
    void search_whenTypeIsInvalid_throwsBadRequestException() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> searchService.search("pop", "invalid_type", 0, 20));
        assertTrue(ex.getMessage().contains("type chỉ nhận all | tracks | albums | artists"));
    }

    @Test
    @DisplayName("Gợi ý từ khóa: Trả về danh sách rỗng khi từ khóa suggest rỗng")
    void suggest_whenQueryIsBlank_returnsEmptyList() {
        List<String> result = searchService.suggest("   ");
        assertTrue(result.isEmpty());
        verifyNoInteractions(cbFactory);
    }

    @Test
    @DisplayName("Circuit Breaker: Khi Elasticsearch gặp sự cố, tự động fallback tìm trong DB của content-service (source = 'database')")
    @SuppressWarnings("unchecked")
    void search_whenElasticsearchFails_fallsBackToContentDatabase() {
        CircuitBreaker mockCircuitBreaker = mock(CircuitBreaker.class);
        when(cbFactory.create("elasticsearch")).thenReturn(mockCircuitBreaker);

        // Giả lập Circuit Breaker bắt ngoại lệ từ Elasticsearch và kích hoạt hàm fallback
        when(mockCircuitBreaker.run(any(Supplier.class), any(Function.class))).thenAnswer(invocation -> {
            Function<Throwable, ?> fallback = invocation.getArgument(1);
            return fallback.apply(new RuntimeException("Elasticsearch connection refused"));
        });

        // Giả lập kết quả trả về từ content-service DB
        TrackSummary mockTrack = new TrackSummary(1, "Bài hát fallback", null, 180, null, 100L,
                LocalDate.now(), 10, "Album test");
        PageResult<TrackSummary> dbPageResult = new PageResult<>(List.of(mockTrack), 0, 20, 1L, 1, true);
        when(contentClient.search(eq("son tung"), eq(0), eq(20))).thenReturn(dbPageResult);

        SearchResponse response = searchService.search("son tung", "all", 0, 20);

        assertNotNull(response);
        assertEquals("database", response.source(), "Nguồn kết quả phải là database do fallback");
        assertEquals("son tung", response.query());
        assertEquals(1, response.tracks().totalElements());
        assertEquals("Bài hát fallback", response.tracks().content().get(0).name());
        verify(contentClient).search(eq("son tung"), eq(0), eq(20));
    }
}
