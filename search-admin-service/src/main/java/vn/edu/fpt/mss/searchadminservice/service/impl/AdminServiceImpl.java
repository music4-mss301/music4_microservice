package vn.edu.fpt.mss.searchadminservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.fpt.mss.searchadminservice.client.AnalyticsClient;
import vn.edu.fpt.mss.searchadminservice.client.ContentClient;
import vn.edu.fpt.mss.searchadminservice.client.EngagementClient;
import vn.edu.fpt.mss.searchadminservice.client.UserClient;
import vn.edu.fpt.mss.searchadminservice.dto.response.DashboardResponse;
import vn.edu.fpt.mss.searchadminservice.service.AdminService;
import vn.edu.fpt.mss.searchadminservice.service.IndexingService;

import java.util.Map;

/** Dashboard tổng hợp: 4 call song song không cần thiết ở quy mô đồ án, mỗi call đều có fallback riêng. */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserClient userClient;
    private final ContentClient contentClient;
    private final EngagementClient engagementClient;
    private final AnalyticsClient analyticsClient;
    private final IndexingService indexingService;

    @Override
    public DashboardResponse dashboard() {
        return new DashboardResponse(userClient.stats(), contentClient.stats(), engagementClient.stats(),
                analyticsClient.stats(), indexingService.status());
    }

    @Override
    public Map<String, Integer> cleanupHistory(int days) {
        return analyticsClient.cleanup(days);
    }
}
