package vn.edu.fpt.mss.mediaservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.mss.mediaservice.entity.MediaFile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    Optional<MediaFile> findByObjectKey(String objectKey);

    boolean existsByObjectKey(String objectKey);

    List<MediaFile> findByObjectKeyIn(Collection<String> keys);

    Page<MediaFile> findByMediaType(String mediaType, Pageable pageable);
}
