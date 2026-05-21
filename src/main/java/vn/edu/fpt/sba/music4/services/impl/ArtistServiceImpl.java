package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.fpt.sba.music4.dto.response.AlbumResponseDTO; // Nhớ import DTO này vào nhé
import vn.edu.fpt.sba.music4.dto.response.ArtistDetailResponseDTO;
import vn.edu.fpt.sba.music4.dto.response.PlaylistResponseDTO;
import vn.edu.fpt.sba.music4.entities.Artist; // Import Entity Artist
import vn.edu.fpt.sba.music4.repositories.ArtistRepository;
import vn.edu.fpt.sba.music4.services.IArtistService;

import java.util.List;

@Service
public class ArtistServiceImpl implements IArtistService {

    private final ArtistRepository artistRepository;

    // Sử dụng Constructor Injection chuẩn chỉnh
    public ArtistServiceImpl(ArtistRepository artistRepository) {
        this.artistRepository = artistRepository;
    }

    @Override
    public List<ArtistDetailResponseDTO> findAll() {
        return artistRepository.findAll()
                .stream()
                .map(this::toDetailResponseDto)
                .toList();
    }
    @Override
    public Page<ArtistDetailResponseDTO> findAll(Pageable pageable) {
        return artistRepository.findAll(pageable).map(this::toDetailResponseDto);
    }
    // Đưa hàm private này vào TRONG class để sử dụng hợp lệ
    private ArtistDetailResponseDTO toDetailResponseDto(Artist artist) {
        // 1. Map danh sách Album sang AlbumResponseDTO
        List<AlbumResponseDTO> albums = artist.getAlbums().stream().map(
                album -> new AlbumResponseDTO(album.getId(), album.getName())
        ).toList();

        // 3. Truyền đầy đủ 5 tham số vào đúng thứ tự của Record
        return new ArtistDetailResponseDTO(
                artist.getId(),
                artist.getName(),
                albums,
                artist.getUserName()
        );
    }
}