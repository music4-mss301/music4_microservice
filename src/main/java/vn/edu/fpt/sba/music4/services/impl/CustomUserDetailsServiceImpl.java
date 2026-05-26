package vn.edu.fpt.sba.music4.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Primary
@RequiredArgsConstructor
public class CustomUserDetailsServiceImpl implements UserDetailsService {
    @Qualifier("userDetailsService")
    private final UserDetailsServiceImpl userService;

    @Qualifier("artistDetailsService")
    private final ArtistDetailsServiceImpl artistService;


    @Override
    public UserDetails loadUserByUsername(String username) {
        try {
            return userService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            return artistService.loadUserByUsername(username);
            // nếu cả hai đều miss → UsernameNotFoundException tự propagate → 401
        }
    }
}
