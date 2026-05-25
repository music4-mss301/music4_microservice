package vn.edu.fpt.sba.music4.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.sba.music4.entities.Playlist;

import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, Integer> {
    List<Playlist> findByUserId(Integer userId);
}
