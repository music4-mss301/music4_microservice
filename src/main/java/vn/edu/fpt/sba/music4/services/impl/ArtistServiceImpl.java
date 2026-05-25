package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.sba.music4.entities.Artist;
import vn.edu.fpt.sba.music4.repositories.ArtistRepository;
import vn.edu.fpt.sba.music4.services.IArtistService;

import java.util.List;

@Service
public class ArtistServiceImpl implements IArtistService {
    private ArtistRepository artistRepository;

    public ArtistServiceImpl(ArtistRepository artistRepository) {
        this.artistRepository = artistRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Artist getArtistById(Integer id) {
        return artistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Nghệ sĩ với ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Artist> getAllArtists() {
        return artistRepository.findAll();
    }
}
