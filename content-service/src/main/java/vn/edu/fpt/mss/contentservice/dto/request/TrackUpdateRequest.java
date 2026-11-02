package vn.edu.fpt.mss.contentservice.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/** null = giữ nguyên. albumId = -1 để gỡ bài khỏi album. artistIds = danh sách nghệ sĩ FEAT. */
public record TrackUpdateRequest(@Size(min = 1, max = 100) String title,
                                 @Positive(message = "Thời lượng phải > 0") Integer duration,
                                 Integer albumId,
                                 List<Integer> categoryIds,
                                 List<Integer> artistIds,
                                 String img) {
}
