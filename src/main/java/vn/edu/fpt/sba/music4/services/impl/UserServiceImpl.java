package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.sba.music4.dtos.request.RegistrationRequest;
import vn.edu.fpt.sba.music4.dtos.request.UserRequest;
import vn.edu.fpt.sba.music4.dtos.response.UserResponseDTO;
import vn.edu.fpt.sba.music4.entities.Artist;
import vn.edu.fpt.sba.music4.entities.Provider;
import vn.edu.fpt.sba.music4.entities.User;
import vn.edu.fpt.sba.music4.entities.UserProvider;
import vn.edu.fpt.sba.music4.enums.ERole;
import vn.edu.fpt.sba.music4.exceptions.AccessDeniedException;
import vn.edu.fpt.sba.music4.repositories.ArtistRepository;
import vn.edu.fpt.sba.music4.repositories.ProviderRepository;
import vn.edu.fpt.sba.music4.repositories.UserProviderRepository;
import vn.edu.fpt.sba.music4.repositories.UserRepository;
import vn.edu.fpt.sba.music4.services.IUserService;
import vn.edu.fpt.sba.music4.util.AWSUtil;
import vn.edu.fpt.sba.music4.util.SecurityUtil;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements IUserService, UserDetailsService {
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final ProviderRepository providerRepository;
    private final UserProviderRepository userProviderRepository;
    private final AWSUtil aWSUtil;

    public UserServiceImpl(UserRepository userRepository, ArtistRepository artistRepository, ProviderRepository providerRepository, UserProviderRepository userProviderRepository, AWSUtil aWSUtil) {
        this.userRepository = userRepository;
        this.artistRepository = artistRepository;
        this.providerRepository = providerRepository;
        this.userProviderRepository = userProviderRepository;
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

        int loggedId = SecurityUtil.getCurrentId();

        if (loggedId != id) {
            throw new AccessDeniedException("You can only update your own profile");
        }

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

    @Override
    public int getTotalUsers() {
        return Math.toIntExact(this.userRepository.count());
    }

    @Override
    public UserResponseDTO getById(Integer userId) {
        return this.userRepository.findById(userId).map(this::toResponseDto).orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
    }

    @Override
    public User processOAuthPostLogin(OAuth2User oAuth2User, String providerName) {
        String providerUserId = oAuth2User.getAttribute("sub"); // ID duy nhất của user trên Google
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture"); // Link ảnh profile

        Provider provider = providerRepository.findByName(providerName.toUpperCase())
                .orElseGet(() -> {
                    Provider newProvider = new Provider();
                    newProvider.setName(providerName.toUpperCase());
                    return providerRepository.save(newProvider);
                });

        Optional<UserProvider> linkOptional = userProviderRepository.findByProviderUserIdAndProvider(providerUserId, provider);

        if (linkOptional.isPresent()) {
            // Đã từng đăng nhập bằng Google trước đây -> Cập nhật lại thông tin mới nhất và trả về User
            User existUser = linkOptional.get().getUser();
            existUser.setName(name);
            existUser.setImg(picture);
            return userRepository.save(existUser);
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            user.setName(name);
            if (user.getImg() == null) user.setImg(picture);
            userRepository.save(user);
        } else {
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setImg(picture);
            user.setRole("LISTENER");
            user.setPassword("");

            String baseUsername = email.split("@")[0];
            user.setUserName(baseUsername + "_" + UUID.randomUUID().toString().substring(0, 5));

            user = userRepository.save(user);
        }

        UserProvider newUserProvider = new UserProvider();
        newUserProvider.setUser(user);
        newUserProvider.setProvider(provider);
        newUserProvider.setProviderUserId(providerUserId);
        userProviderRepository.save(newUserProvider);

        return user;
    }


}
