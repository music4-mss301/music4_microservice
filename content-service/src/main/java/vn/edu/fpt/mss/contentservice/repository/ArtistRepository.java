package vn.edu.fpt.mss.contentservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.mss.contentservice.entity.Artist;

import java.util.List;
import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, Integer> {

    Optional<Artist> findByUserIdAndIsDeletedFalse(Integer userId);

    Optional<Artist> findByIdAndIsDeletedFalse(Integer id);

    boolean existsByUserId(Integer userId);

    List<Artist> findByIsDeletedFalseOrderByNameAsc();

    @Query("""
            SELECT a FROM Artist a WHERE a.isDeleted = false
              AND (CAST(:keyword AS string) IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            """)
    Page<Artist> search(@Param("keyword") String keyword, Pageable pageable);

    long countByIsDeletedFalse();

    List<Artist> findByIdIn(java.util.Collection<Integer> ids);
}
