package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.sba.music4.entities.Album;
import vn.edu.fpt.sba.music4.entities.Artist;
import vn.edu.fpt.sba.music4.repositories.AlbumRepository;
import vn.edu.fpt.sba.music4.services.IAlbumService;

import java.util.List;

@Service
public class AlbumServiceImpl implements IAlbumService {
    private AlbumRepository albumRepository;

    public AlbumServiceImpl(AlbumRepository albumRepository) {
        this.albumRepository = albumRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Album> getAlbumsByArtist(Integer artistId) {
        return albumRepository.findByArtistId(artistId);
    }

    @Override
    @Transactional
    public Album createAlbum(Album album) {
        if (album.getName() == null || album.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên album không được để trống!");
        }

        // Giá trị mặc định trackTotal = 0 và uploadDate tự động sinh nhờ Entity đã cấu hình
        return albumRepository.save(album);
    }

    @Override
    @Transactional
    public void deleteAlbum(Integer id) {
        Album album = albumRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy album với ID: " + id));
        albumRepository.delete(album);
    }
}
