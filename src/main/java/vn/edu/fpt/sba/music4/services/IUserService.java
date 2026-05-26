package vn.edu.fpt.sba.music4.services;

import vn.edu.fpt.sba.music4.entities.User;

import java.util.Optional;

public interface IUserService {
    Optional<User> getByUsername (String username);
}


