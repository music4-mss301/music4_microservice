package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.entities.Comment;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {
    List<Comment> findByTrackIdOrderByCreatedAtDesc(Integer trackId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.artist.id = :artistId OR c.track.id IN (SELECT ta.track.id FROM TrackArtist ta WHERE ta.artist.id = :artistId AND UPPER(ta.role) = 'MAIN')")
    void deleteCommentsByArtistOrMainTrack(@Param("artistId") Integer artistId); //[cite: 51]



}