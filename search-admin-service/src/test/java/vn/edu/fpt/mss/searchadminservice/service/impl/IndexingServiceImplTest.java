package vn.edu.fpt.mss.searchadminservice.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.fpt.mss.searchadminservice.client.AiClient;
import vn.edu.fpt.mss.searchadminservice.client.ContentClient;
import vn.edu.fpt.mss.searchadminservice.config.IndexNames;
import vn.edu.fpt.mss.searchadminservice.dto.response.ReindexStatus;
import vn.edu.fpt.mss.searchadminservice.es.EsClient;
import vn.edu.fpt.mss.searchadminservice.es.IndexInitializer;
import vn.edu.fpt.mss.searchadminservice.exception.BusinessException;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IndexingServiceImplTest {

    @Mock
    private EsClient es;

    @Mock
    private AiClient aiClient;

    @Mock
    private ContentClient contentClient;

    @Mock
    private IndexInitializer indexInitializer;

    @InjectMocks
    private IndexingServiceImpl indexingService;

    @Test
    @DisplayName("Rule: Reindex chỉ được chạy 1 lần tại một thời điểm, gọi tiếp khi đang chạy ném BusinessException (409)")
    void startReindex_whenAlreadyRunning_throwsBusinessException() {
        // Giả lập trạng thái reindexing đang chạy (true)
        ReflectionTestUtils.setField(indexingService, "reindexing", new AtomicBoolean(true));

        BusinessException exception = assertThrows(BusinessException.class, () -> indexingService.startReindex());
        assertTrue(exception.getMessage().contains("Đang reindex"));
    }

    @Test
    @DisplayName("Thao tác: deleteTrack gọi lệnh xóa document đúng index trong Elasticsearch")
    void deleteTrack_callsEsDeleteWithCorrectIndexAndId() {
        indexingService.deleteTrack(123);

        verify(es).delete(IndexNames.TRACKS, "123");
    }

    @Test
    @DisplayName("Thao tác: deleteAlbum gọi lệnh xóa document đúng index trong Elasticsearch")
    void deleteAlbum_callsEsDeleteWithCorrectIndexAndId() {
        indexingService.deleteAlbum(456);

        verify(es).delete(IndexNames.ALBUMS, "456");
    }

    @Test
    @DisplayName("Thao tác: renameCategory gọi updateByQuery cập nhật tên thể loại cho các bài hát liên quan")
    void renameCategory_callsEsUpdateByQuery() {
        indexingService.renameCategory(10, "Acoustic Pop");

        verify(es).updateByQuery(eq(IndexNames.TRACKS), anyMap());
    }

    @Test
    @DisplayName("Trạng thái: status() trả về trạng thái reindex không rỗng")
    void status_returnsReindexStatus() {
        ReindexStatus status = indexingService.status();
        assertNotNull(status);
        assertFalse(status.running());
    }
}
