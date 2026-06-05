package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.dtos.request.RegistrationRequest;
import vn.edu.fpt.sba.music4.dtos.request.UserRequest;
import vn.edu.fpt.sba.music4.entities.User;

import java.util.Optional;

public interface IUserService {
    Optional<User> getByUsername (String username);
    User save(RegistrationRequest registrationRequest);
    User update(Integer userId, UserRequest  userRequest);
}


