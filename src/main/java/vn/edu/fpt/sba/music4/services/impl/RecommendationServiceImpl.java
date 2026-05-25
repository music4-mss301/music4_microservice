//package vn.edu.fpt.sba.music4.services.impl;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.stereotype.Service;
//import vn.edu.fpt.sba.music4.dtos.response.TrackSuggestResponse;
//import vn.edu.fpt.sba.music4.entities.Track;
//import vn.edu.fpt.sba.music4.repositories.TrackRepository;
//import vn.edu.fpt.sba.music4.services.IRecommendationService;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class RecommendationServiceImpl implements IRecommendationService {
//
//    private final TrackRepository trackRepository;
//
//    public List<TrackSuggestResponse> getRecommendationsForUser(Integer userId) {
//        log.info("Generating recommendations for user: {}", userId != null ? userId : "Guest");
//
//        List<Track> topTracks = trackRepository.findTopTrendingTracks(PageRequest.of(0, 10));
//
//        return topTracks.stream().map(track -> new TrackSuggestResponse(
//                track.getId(),
//                track.getName(),
//                track.getImg(),
//                track.getDuration(),
//                track.getPreviewPath(),
//                track.getViewCount(),
//                track.getViewCount() * 0.8 + 15.0 // Điểm matchScore mô phỏng
//        )).collect(Collectors.toList());
//    }
//}