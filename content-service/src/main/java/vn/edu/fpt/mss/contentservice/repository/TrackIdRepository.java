package vn.edu.fpt.mss.contentservice.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import vn.edu.fpt.mss.contentservice.entity.Track;

import java.util.List;

/** Chỉ lấy id (nhẹ) để xáo trộn theo seed cho trang khám phá ngẫu nhiên. */
public interface TrackIdRepository extends Repository<Track, Integer> {

    @Query("SELECT t.id FROM Track t ORDER BY t.id")
    List<Integer> findAllIds();
}
