package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import vn.edu.fpt.sba.music4.dtos.request.RegistrationRequest;
import vn.edu.fpt.sba.music4.entities.User;
import vn.edu.fpt.sba.music4.enums.ERole;
import vn.edu.fpt.sba.music4.repositories.ArtistRepository;
import vn.edu.fpt.sba.music4.repositories.UserRepository;
import vn.edu.fpt.sba.music4.services.IUserService;

import java.util.Optional;

@Service
public class UserServiceImpl implements IUserService, UserDetailsService {
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;

    public UserServiceImpl(UserRepository userRepository, ArtistRepository artistRepository) {
        this.userRepository = userRepository;
        this.artistRepository = artistRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return this.userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException(username + " not found"));
    }

    @Override
    public Optional<User> getByUsername(String username) {
        return this.userRepository.findByUserName(username);
    }

    @Override
    public User save(RegistrationRequest registrationRequest) {
        if (registrationRequest.username() == null || registrationRequest.username().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (registrationRequest.password() == null || registrationRequest.password().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        if (userRepository.findByUserName(registrationRequest.username()).isPresent()
                || artistRepository.findByUserName(registrationRequest.username()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        String hashedPassword = BCrypt.hashpw(registrationRequest.password(), BCrypt.gensalt()); // TODO: Hash password before saving to database)

        return this.userRepository.save(new User(registrationRequest.name(),
                "null",
                registrationRequest.email(),
                registrationRequest.username(),
                hashedPassword,
                ERole.LISTENER.name()));
    }
}
