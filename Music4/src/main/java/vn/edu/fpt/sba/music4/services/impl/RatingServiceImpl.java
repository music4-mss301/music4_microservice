package vn.edu.fpt.sba.music4.services.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.fpt.sba.music4.dtos.request.RatingRequest;
import vn.edu.fpt.sba.music4.entities.Rating;
import vn.edu.fpt.sba.music4.repositories.RatingRepository;
import vn.edu.fpt.sba.music4.repositories.TrackRepository;
import vn.edu.fpt.sba.music4.repositories.UserRepository;
import vn.edu.fpt.sba.music4.services.IRatingService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements IRatingService {

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
}
