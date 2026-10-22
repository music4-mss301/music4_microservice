package vn.edu.fpt.mss.searchadminservice.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.fpt.mss.searchadminservice.config.IndexNames;
import vn.edu.fpt.mss.searchadminservice.dto.request.CurationRequest;
import vn.edu.fpt.mss.searchadminservice.dto.response.CurationResponse;
import vn.edu.fpt.mss.searchadminservice.es.EsClient;
import vn.edu.fpt.mss.searchadminservice.exception.BusinessException;
import vn.edu.fpt.mss.searchadminservice.exception.ResourceNotFoundException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurationServiceImplTest {

    @Mock
    private EsClient es;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CurationServiceImpl curationService;

    private JsonNode emptySearchResult;
    private JsonNode foundSearchResult;
    private JsonNode curationDocNode;

    @BeforeEach
    void setUp() throws Exception {
        emptySearchResult = objectMapper.readTree("{\"hits\": {\"hits\": []}}");
        foundSearchResult = objectMapper.readTree("""
            {
                "hits": {
                    "hits": [
                        {
                            "_id": "cur-1",
                            "_source": {
                                "keyword": "chill",
                                "trackIds": [1, 2],
                                "note": "Nhac chill",
                                "createdAt": "2026-10-21T10:00:00Z"
                            }
                        }
                    ]
                }
            }
        """);
        curationDocNode = objectMapper.readTree("""
            {
                "keyword": "chill",
                "trackIds": [1, 2],
                "note": "Nhac chill",
                "createdAt": "2026-10-21T10:00:00Z"
            }
        """);
    }

    @Test
    @DisplayName("Rule: Tạo Curation thành công khi từ khóa chưa tồn tại và loại bỏ trackIds trùng lặp")
    void create_whenKeywordNotExists_createsSuccessfullyAndDeduplicatesTrackIds() {
        when(es.search(eq(IndexNames.CURATIONS), anyMap())).thenReturn(emptySearchResult);
        when(es.get(eq(IndexNames.CURATIONS), anyString())).thenReturn(Optional.of(curationDocNode));

        // Given: trackIds có chứa phần tử trùng lặp: [1, 2, 2, 1]
        CurationRequest request = new CurationRequest("CHILL", List.of(1, 2, 2, 1), "Nhac chill");

        CurationResponse response = curationService.create(request);

        assertNotNull(response);
        assertEquals("chill", response.keyword());

        // Verify ES put được gọi với danh sách trackIds đã được loại bỏ trùng
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(es).put(eq(IndexNames.CURATIONS), anyString(), captor.capture());
        
        Map<String, Object> savedDoc = captor.getValue();
        assertEquals("chill", savedDoc.get("keyword"));
        @SuppressWarnings("unchecked")
        List<Integer> savedTrackIds = (List<Integer>) savedDoc.get("trackIds");
        assertEquals(List.of(1, 2), savedTrackIds, "trackIds phải được loại bỏ trùng lặp");
    }

    @Test
    @DisplayName("Rule: Ném BusinessException (409) khi tạo Curation có từ khóa đã tồn tại")
    void create_whenKeywordAlreadyExists_throwsBusinessException() {
        when(es.search(eq(IndexNames.CURATIONS), anyMap())).thenReturn(foundSearchResult);

        CurationRequest request = new CurationRequest("chill", List.of(1, 2), "Test trung");

        BusinessException exception = assertThrows(BusinessException.class, () -> curationService.create(request));
        assertTrue(exception.getMessage().contains("đã có curation"));
        verify(es, never()).put(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Rule: Ném ResourceNotFoundException (404) khi tìm Curation không tồn tại")
    void get_whenNotFound_throwsResourceNotFoundException() {
        when(es.get(eq(IndexNames.CURATIONS), eq("non-existent-id"))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> curationService.get("non-existent-id"));
    }

    @Test
    @DisplayName("Rule: Cập nhật Curation thất bại (409) nếu đổi sang từ khóa thuộc Curation khác")
    void update_whenKeywordBelongsToAnotherCuration_throwsBusinessException() {
        // Curation hiện tại có id = "my-curation-id"
        JsonNode existingDoc = curationDocNode;
        when(es.get(eq(IndexNames.CURATIONS), eq("my-curation-id"))).thenReturn(Optional.of(existingDoc));
        
        // Tìm thấy từ khóa này đã thuộc về Curation khác có id = "cur-1" (từ foundSearchResult)
        when(es.search(eq(IndexNames.CURATIONS), anyMap())).thenReturn(foundSearchResult);

        CurationRequest updateRequest = new CurationRequest("chill", List.of(3, 4), "Doi tu khoa");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> curationService.update("my-curation-id", updateRequest));
        assertTrue(ex.getMessage().contains("đã có curation khác"));
        verify(es, never()).put(anyString(), eq("my-curation-id"), any());
    }
}
