package vn.edu.fpt.mss.contentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.mss.contentservice.entity.TrackArtist;

public interface TrackArtistRepository extends JpaRepository<TrackArtist, Integer> {

    long countByArtistId(Integer artistId);
}
