package vn.edu.fpt.sba.music4.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.fpt.sba.music4.dtos.request.RegistrationRequest;
import vn.edu.fpt.sba.music4.dtos.request.UserRequest;
import vn.edu.fpt.sba.music4.dtos.response.UserResponseDTO;
import vn.edu.fpt.sba.music4.entities.User;

import java.util.List;
import java.util.Optional;

public interface IUserService {
    Optional<User> getByUsername (String username);
    User save(RegistrationRequest registrationRequest);
    UserResponseDTO update(Integer userId, UserRequest  userRequest);
    Page<UserResponseDTO> getAll(Pageable pageable);
    int getTotalUsers();
}


