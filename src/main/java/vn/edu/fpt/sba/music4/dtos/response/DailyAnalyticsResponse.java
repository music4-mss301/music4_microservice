package vn.edu.fpt.sba.music4.dtos.response;

import java.time.LocalDate;

public record DailyAnalyticsResponse(
        LocalDate day,
        Long views,
        Long likes
) {
}
