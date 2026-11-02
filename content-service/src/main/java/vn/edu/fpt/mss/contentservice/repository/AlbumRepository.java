package vn.edu.fpt.mss.contentservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.mss.contentservice.entity.Album;

import java.util.List;

public interface AlbumRepository extends JpaRepository<Album, Integer> {

    @EntityGraph(attributePaths = "artist")
    List<Album> findByArtistIdOrderByUploadDateDesc(Integer artistId);

    @EntityGraph(attributePaths = "artist")
    Page<Album> findAllBy(Pageable pageable);

    int countByArtistId(Integer artistId);

    boolean existsByArtistIdAndNameIgnoreCase(Integer artistId, String name);

    boolean existsByArtistIdAndNameIgnoreCaseAndIdNot(Integer artistId, String name, Integer id);
}
