package vn.edu.fpt.sba.music4.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.fpt.sba.music4.dtos.response.ArtistOverviewResponse;
import vn.edu.fpt.sba.music4.dtos.response.IChartData;
import vn.edu.fpt.sba.music4.repositories.ArtistRepository;
import vn.edu.fpt.sba.music4.repositories.HistoryRepository;
import vn.edu.fpt.sba.music4.services.IAnalyticsService;

import java.util.List;

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

        return new ArtistOverviewResponse(totalViews, totalFavorites, totalFollowers);
    }
}
