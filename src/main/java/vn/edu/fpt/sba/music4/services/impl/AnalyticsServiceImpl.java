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

import java.util.List;
import java.util.Map;

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

        List<DailyAnalyticsResponse> chartData =
                historyRepository.getDailyAnalyticsByArtist(artistId)
                        .stream()
                        .map(item -> new DailyAnalyticsResponse(
                                ((java.sql.Date) item.get("day")).toLocalDate(),
                                ((Number) item.get("views")).longValue(),
                                0L
                        ))
                        .toList();

        List<TopTrackResponse> topTracks =
                artistRepository.getTopTracksByArtist(artistId)
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
