package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.entities.TrackArtist;

import java.util.List;

@Repository
public interface TrackArtistRepository extends JpaRepository<TrackArtist, Integer> {
    long countByArtistId(Integer artistId);
    List<TrackArtist> findByTrackId(Integer trackId);
    @Query("SELECT ta FROM TrackArtist ta JOIN FETCH ta.artist WHERE ta.track.id = :trackId")
    List<TrackArtist> findByTrackIdWithArtist(@Param("trackId") Integer trackId);
}
