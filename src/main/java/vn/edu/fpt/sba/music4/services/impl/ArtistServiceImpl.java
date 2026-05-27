package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.sba.music4.dtos.request.RegistrationRequest;
import vn.edu.fpt.sba.music4.entities.Artist;
import vn.edu.fpt.sba.music4.repositories.ArtistRepository;
import vn.edu.fpt.sba.music4.repositories.UserRepository;
import vn.edu.fpt.sba.music4.services.IArtistService;

import java.util.List;

@Service
public class ArtistServiceImpl implements IArtistService, UserDetailsService {
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;

    public ArtistServiceImpl(ArtistRepository artistRepository, UserRepository userRepository) {
        this.artistRepository = artistRepository;
        this.userRepository = userRepository;
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

    @Override
    @Transactional(readOnly = true)
    public List<Artist> getAllArtists() {
        return artistRepository.findAll();
    }

    @Override
    @Transactional
    public Artist save(RegistrationRequest registrationRequest) {
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

        Artist artist = new Artist();
        artist.setName(registrationRequest.name() == null ? "Unknown Artist" : registrationRequest.name());
        artist.setImg("null"); // or null
        artist.setEmail(registrationRequest.email());
        artist.setPassword(hashedPassword);
        artist.setUserName(registrationRequest.username());
        artist.setTrackTotal(0);
        artist.setAlbumTotal(0);

        return artistRepository.save(artist);
    }
}
