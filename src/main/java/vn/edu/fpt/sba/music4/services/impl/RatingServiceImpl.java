package vn.edu.fpt.sba.music4.services.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.fpt.sba.music4.dtos.request.RatingRequest;
import vn.edu.fpt.sba.music4.dtos.response.RatingResponseDTO;
import vn.edu.fpt.sba.music4.entities.Rating;
import vn.edu.fpt.sba.music4.repositories.RatingRepository;
import vn.edu.fpt.sba.music4.repositories.TrackRepository;
import vn.edu.fpt.sba.music4.repositories.UserRepository;
import vn.edu.fpt.sba.music4.services.IRatingService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public
class RatingServiceImpl implements IRatingService {

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final TrackRepository trackRepository;

    @Override
    @Transactional
    public void saveRating(RatingRequest request) {
        Optional<Rating> existingRating = ratingRepository.findByUserIdAndTrackId(request.userId(), request.trackId());

        if (existingRating.isPresent()) {
            Rating rating = existingRating.get();
            rating.setRatingValue(request.ratingValue());
            ratingRepository.save(rating);
        } else {
            Rating newRating = new Rating();
            newRating.setUser(userRepository.getReferenceById(request.userId()));
            newRating.setTrack(trackRepository.getReferenceById(request.trackId()));
            newRating.setRatingValue(request.ratingValue());

            ratingRepository.save(newRating);
        }
    }
    @Override
    public int getRatingByUserAndTrack(Integer userId, Integer trackId) {

        return ratingRepository.findByUserIdAndTrackId(userId, trackId)
                .map(Rating::getRatingValue)
                .orElse(0);
    }
    @Override
    public double getAverageRating(Integer trackId) {
        Double avg = ratingRepository.getAverageRatingByTrackId(trackId);
        return (avg != null) ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    @Override
    public long countRatings(Integer trackId) {
        return ratingRepository.countByTrackId(trackId);
    }
    @Override
    public RatingResponseDTO getRatingStats(Integer userId, Integer trackId) {
        double avg = getAverageRating(trackId);
        long count = countRatings(trackId);
        int myRating = getRatingByUserAndTrack(userId, trackId);

        return new RatingResponseDTO(avg, myRating, count);
    }
}
