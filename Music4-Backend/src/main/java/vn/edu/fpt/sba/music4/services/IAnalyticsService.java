package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.dtos.response.ArtistOverviewResponse;
import vn.edu.fpt.sba.music4.dtos.response.IChartData;

import java.util.List;

public interface IAnalyticsService {
    public List<IChartData> getArtistDailyViews(Integer artistId);

    ArtistOverviewResponse getArtistOverview(Integer artistId);
}
