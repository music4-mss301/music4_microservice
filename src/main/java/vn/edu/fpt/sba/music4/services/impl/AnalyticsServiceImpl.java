package vn.edu.fpt.sba.music4.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.fpt.sba.music4.dtos.response.ArtistOverviewResponse;
import vn.edu.fpt.sba.music4.dtos.response.DailyAnalyticsResponse;
import vn.edu.fpt.sba.music4.dtos.response.IChartData;
import vn.edu.fpt.sba.music4.dtos.response.TopTrackResponse;
import vn.edu.fpt.sba.music4.repositories.ArtistRepository;
import vn.edu.fpt.sba.music4.repositories.HistoryRepository;
import vn.edu.fpt.sba.music4.services.IAnalyticsService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements IAnalyticsService {

    private final HistoryRepository  historyRepository;
    private final ArtistRepository artistRepository;


    @Override
    public List<IChartData> getArtistDailyViews(Integer artistId) {
        return historyRepository.countViewsByArtistPerDay(artistId);
    }

    @Override
    public ArtistOverviewResponse getArtistOverview(Integer artistId) {
        Long totalViews = artistRepository.getTotalViewsByArtist(artistId);
        Long totalFavorites = artistRepository.getTotalFavoritesByArtist(artistId);
        Long totalFollowers = artistRepository.getTotalFollowersByArtist(artistId);
        Long totalComments = artistRepository.getTotalCommentsByArtist(artistId);

        // Lấy dữ liệu Lượt nghe và Lượt thích
        List<Map<String, Object>> rawViews = historyRepository.getDailyViewsByArtist(artistId);
        List<Map<String, Object>> rawLikes = historyRepository.getDailyLikesByArtist(artistId);

        // Trộn dữ liệu theo ngày sử dụng TreeMap (để tự động sắp xếp ngày tăng dần)
        Map<LocalDate, DailyAnalyticsResponse> mergedData = new TreeMap<>();

        // Map Lượt nghe
        for (Map<String, Object> row : rawViews) {
            LocalDate date = ((java.sql.Date) row.get("day")).toLocalDate();
            Long views = ((Number) row.get("views")).longValue();
            mergedData.put(date, new DailyAnalyticsResponse(date, views, 0L));
        }

        // Map Lượt thích đè lên (hoặc tạo mới nếu ngày đó có thích nhưng không có nghe)
        for (Map<String, Object> row : rawLikes) {
            LocalDate date = ((java.sql.Date) row.get("day")).toLocalDate();
            Long likes = ((Number) row.get("likes")).longValue();

            DailyAnalyticsResponse existing = mergedData.getOrDefault(date, new DailyAnalyticsResponse(date, 0L, 0L));
            mergedData.put(date, new DailyAnalyticsResponse(date, existing.views(), likes));
        }

        List<DailyAnalyticsResponse> chartData = new ArrayList<>(mergedData.values());

        List<TopTrackResponse> topTracks = artistRepository.getTopTracksByArtist(artistId)
                .stream()
                .map(item -> new TopTrackResponse(
                        ((Number) item.get("id")).longValue(),
                        (String) item.get("name"),
                        ((Number) item.get("view_count")).intValue(),
                        ((Number) item.get("favorite_count")).intValue()
                ))
                .toList();

        return new ArtistOverviewResponse(
                totalViews,
                totalFavorites,
                totalFollowers,
                totalComments,
                chartData,
                topTracks
        );
    }
}
