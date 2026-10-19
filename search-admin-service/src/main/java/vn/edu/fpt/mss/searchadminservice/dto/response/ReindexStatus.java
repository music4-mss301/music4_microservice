package vn.edu.fpt.mss.searchadminservice.dto.response;

import java.time.OffsetDateTime;

public record ReindexStatus(boolean running, OffsetDateTime lastStartedAt, OffsetDateTime lastFinishedAt,
                            int lastIndexed, String lastError) {
}
