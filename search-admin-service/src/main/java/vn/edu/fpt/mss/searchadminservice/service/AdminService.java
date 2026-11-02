package vn.edu.fpt.mss.searchadminservice.service;

import vn.edu.fpt.mss.searchadminservice.dto.response.DashboardResponse;

import java.util.Map;

public interface AdminService {
    DashboardResponse dashboard();

    Map<String, Integer> cleanupHistory(int days);
}
