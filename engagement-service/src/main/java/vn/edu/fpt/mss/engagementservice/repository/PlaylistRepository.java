package vn.edu.fpt.mss.engagementservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.mss.engagementservice.entity.Playlist;

import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, Integer> {

    List<Playlist> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<Playlist> findByUserIdAndIsPublicTrueOrderByCreatedAtDesc(Integer userId);

    boolean existsByUserIdAndNameIgnoreCase(Integer userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Integer userId, String name, Integer id);

    long countByUserId(Integer userId);

    @Modifying
    @Query("DELETE FROM Playlist p WHERE p.userId = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
}
