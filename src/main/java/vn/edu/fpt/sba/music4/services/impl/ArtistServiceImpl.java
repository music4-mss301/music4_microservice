package vn.edu.fpt.sba.music4.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.sba.music4.dtos.request.ArtistRequestDTO;
import vn.edu.fpt.sba.music4.dtos.request.RegistrationRequest;
import vn.edu.fpt.sba.music4.dtos.response.ArtistResponseDTO;
import vn.edu.fpt.sba.music4.entities.Artist;
import vn.edu.fpt.sba.music4.repositories.ArtistRepository;
import vn.edu.fpt.sba.music4.repositories.UserRepository;
import vn.edu.fpt.sba.music4.services.IArtistService;
import vn.edu.fpt.sba.music4.util.AWSUtil;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArtistServiceImpl implements IArtistService, UserDetailsService {
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final AWSUtil aWSUtil;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.artistRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException(username + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public ArtistResponseDTO getArtistById(Integer id) { // 🟢 Đổi kiểu trả về ở đây
        Artist artist = artistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Nghệ sĩ với ID: " + id));

        // 🟢 Gọi hàm map sang DTO để loại bỏ dữ liệu nhạy cảm & chống lặp JSON
        return toResponseDto(artist);
    }

    public ArtistResponseDTO toResponseDto(Artist artist) {
        if (artist == null) return null;

        return new ArtistResponseDTO(
                artist.getId(),
                artist.getName(),
                artist.getImg(),
                artist.getCover(),
                artist.getEmail(),
                artist.getUsername(),
                artist.getTrackTotal(),
                artist.getAlbumTotal()
        );
    }

    @Override
    public Page<ArtistResponseDTO> getAllArtists(Pageable pageable) {
        Page<Artist> artistPage = artistRepository.findAll(pageable);
        return artistPage.map(artist -> this.toResponseDto(artist));
    }

    @Override
    @Transactional
    public Artist save(RegistrationRequest registrationRequest) {
        Artist artist = new Artist();
        MultipartFile file = registrationRequest.img();

        if (file != null && !file.isEmpty()) {
            try {
                String s3Key = aWSUtil.uploadFile(file, "avatars");
                artist.setImg(s3Key);
            } catch (IOException e) {
                throw new RuntimeException("Upload image failed", e);
            }
        } else {
            artist.setImg(null); // or set a default image key if you have one
        }

        // basic validation
        if (registrationRequest.username() == null || registrationRequest.username().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (registrationRequest.password() == null || registrationRequest.password().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        // check username collisions in both USERS and ARTISTS
        if (userRepository.findByUserName(registrationRequest.username()).isPresent()
                || artistRepository.findByUserName(registrationRequest.username()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        // optional: check email collision (add findByEmail methods if you want stricter validation)

        // hash password
        String hashedPassword = BCrypt.hashpw(registrationRequest.password(), BCrypt.gensalt());

        artist.setName(registrationRequest.name() == null ? "Unknown Artist" : registrationRequest.name());

        artist.setEmail(registrationRequest.email());
        artist.setPassword(hashedPassword);
        artist.setUserName(registrationRequest.username());
        artist.setTrackTotal(0);
        artist.setAlbumTotal(0);

        return artistRepository.save(artist);
    }

    @Override
    @Transactional
    public ArtistResponseDTO update(Integer artistId, ArtistRequestDTO artistRequest) {
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new RuntimeException("Artist not found with id: " + artistId));

        // Update name if provided and not blank
        if (artistRequest.name() != null && !artistRequest.name().isBlank()) {
            artist.setName(artistRequest.name());
        }

        // Update avatar if provided
        MultipartFile avatarFile = artistRequest.img();
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String s3Key = aWSUtil.uploadFile(avatarFile, "avatars");
                artist.setImg(s3Key);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload avatar image", e);
            }
        }

        // Update cover if provided
        MultipartFile coverFile = artistRequest.cover();
        if (coverFile != null && !coverFile.isEmpty()) {
            try {
                String s3Key = aWSUtil.uploadFile(coverFile, "covers");
                artist.setCover(s3Key);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload cover image", e);
            }
        }

        Artist updatedArtist = artistRepository.save(artist);
        return toResponseDto(updatedArtist);
    }
}
