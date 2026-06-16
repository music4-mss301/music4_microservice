package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.sba.music4.dtos.request.RegistrationRequest;
import vn.edu.fpt.sba.music4.dtos.request.UserRequest;
import vn.edu.fpt.sba.music4.dtos.response.UserResponseDTO;
import vn.edu.fpt.sba.music4.entities.User;
import vn.edu.fpt.sba.music4.enums.ERole;
import vn.edu.fpt.sba.music4.repositories.ArtistRepository;
import vn.edu.fpt.sba.music4.repositories.UserRepository;
import vn.edu.fpt.sba.music4.services.IUserService;
import vn.edu.fpt.sba.music4.util.AWSUtil;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements IUserService, UserDetailsService {
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final AWSUtil aWSUtil;

    public UserServiceImpl(UserRepository userRepository, ArtistRepository artistRepository, AWSUtil aWSUtil) {
        this.userRepository = userRepository;
        this.artistRepository = artistRepository;
        this.aWSUtil = aWSUtil;
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

    private UserResponseDTO toResponseDto(User user) {
        if (user == null) return null;

        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getImg(),
                user.getEmail(),
                user.getGender(),
                user.getRole(),
                user.getUsername()
        );
    }

    @Override
    public UserResponseDTO update(Integer id, UserRequest userRequest) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        System.out.println("Current user: " + userDetails.getUsername() + " - " + userDetails.getAuthorities());

        User user = this.userRepository.findById(id).map((u) -> {
                    if(userRequest.name() != null && !userRequest.name().isBlank()) {
                        u.setName(userRequest.name());
                    }

                    if(userRequest.gender() != null){
                        u.setGender(userRequest.gender());
                    }
                    MultipartFile file = userRequest.img();

                    if (file != null && !file.isEmpty()) {
                        try {
                            String s3Key = aWSUtil.uploadFile(file, "avatars");
                            u.setImg(s3Key);
                        } catch (IOException e) {
                            throw new RuntimeException("Upload image failed", e);
                        }
                    }
                    return this.userRepository.save(u);
                })
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return toResponseDto(user);
    }

    @Override
    public Page<UserResponseDTO> getAll(Pageable pageable) {
        return this.userRepository.findAll(pageable).map(this::toResponseDto);
    }


}
