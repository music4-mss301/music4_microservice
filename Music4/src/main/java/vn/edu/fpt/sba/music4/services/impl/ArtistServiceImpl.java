package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
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
public class ArtistServiceImpl implements IArtistService, UserDetailsService {
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final AWSUtil aWSUtil;

    public ArtistServiceImpl(UserRepository userRepository, ArtistRepository artistRepository, AWSUtil aWSUtil) {
        this.userRepository = userRepository;
        this.artistRepository = artistRepository;
        this.aWSUtil = aWSUtil;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.artistRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException(username + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Artist getArtistById(Integer id) {
        return artistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Nghệ sĩ với ID: " + id));
    }

    public ArtistResponseDTO toResponseDto(Artist artist) {
        if (artist == null) return null;

        return new ArtistResponseDTO(
                artist.getId(),
                artist.getName(),
                artist.getImg(),
                artist.getUsername(),
                artist.getTrackTotal(),
                artist.getAlbumTotal()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArtistResponseDTO> getAllArtists() {
        return artistRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
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
}
