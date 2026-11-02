package vn.edu.fpt.mss.analyticsservice.dto.response;

import vn.edu.fpt.mss.analyticsservice.dto.client.TrackSummary;

public record TopTrackResponse(Integer trackId, long plays, TrackSummary track) {
}
