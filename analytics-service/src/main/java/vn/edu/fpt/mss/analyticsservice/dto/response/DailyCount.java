package vn.edu.fpt.mss.analyticsservice.dto.response;

import java.time.LocalDate;

public record DailyCount(LocalDate day, Long total) {
}
