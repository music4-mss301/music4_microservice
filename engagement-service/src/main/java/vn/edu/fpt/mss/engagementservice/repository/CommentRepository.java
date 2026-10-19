package vn.edu.fpt.mss.engagementservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.mss.engagementservice.entity.Comment;

import java.time.LocalDateTime;

public interface CommentRepository extends JpaRepository<Comment, Integer> {

    Page<Comment> findByTrackIdOrderByCreatedAtDesc(Integer trackId, Pageable pageable);

    long countByUserIdAndCreatedAtAfter(Integer userId, LocalDateTime after);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.trackId = :trackId")
    int deleteByTrackId(@Param("trackId") Integer trackId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.userId = :userId")
    int deleteByUserId(@Param("userId") Integer userId);
}
