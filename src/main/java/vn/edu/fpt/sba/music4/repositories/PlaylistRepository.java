package vn.edu.fpt.sba.music4.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.sba.music4.entities.Playlist;

import java.util.List;
import java.util.Optional;


@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Integer> {
    List<Playlist> findByUserId(Integer userId);
    @Query("SELECT p FROM Playlist p JOIN FETCH p.user WHERE p.id = :id")
    Optional<Playlist> findById(@Param("id") Integer id);
    @Query(value = "SELECT * FROM PLAYLISTS WHERE REPLACE(LOWER(name), '-', ' ') COLLATE Latin1_General_CI_AI LIKE N'%' + :keyword + '%' ORDER BY name ASC",
            countQuery = "SELECT COUNT(*) FROM PLAYLISTS WHERE REPLACE(LOWER(name), '-', ' ') COLLATE Latin1_General_CI_AI LIKE N'%' + :keyword + '%'",
            nativeQuery = true)
    Page<Playlist> searchByName(@Param("keyword") String keyword, Pageable pageable);
    @Modifying
    @Query("DELETE FROM Playlist p WHERE p.user.id = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
}

