package vn.edu.fpt.mss.searchadminservice.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record CurationResponse(String id, String keyword, List<Integer> trackIds, String note,
                               OffsetDateTime createdAt) {
}
