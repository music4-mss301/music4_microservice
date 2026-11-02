package vn.edu.fpt.mss.analyticsservice.dto.response;

import java.util.List;

public record AdminOverviewResponse(long totalPlays, long playsToday, long playsLast7Days,
                                    long listenersLast7Days, List<DailyCount> dailyPlays) {
}
