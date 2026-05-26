package vn.edu.fpt.sba.music4.services.impl;

import org.springframework.stereotype.Service;
import vn.edu.fpt.sba.music4.entities.User;
import vn.edu.fpt.sba.music4.repositories.UserRepository;
import vn.edu.fpt.sba.music4.services.IUserService;

import java.util.Optional;

@Service
public class UserServiceImpl implements IUserService {
    private final UserRepository userRepository;
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Override
    public Optional<User> getByUsername(String username) {
        return this.userRepository.findByUserName(username);
    }
}
