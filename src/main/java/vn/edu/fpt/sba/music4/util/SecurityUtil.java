package vn.edu.fpt.sba.music4.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import vn.edu.fpt.sba.music4.entities.Artist;
import vn.edu.fpt.sba.music4.entities.User;

@Component
public class SecurityUtil {

    public static UserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails ud) {
            return ud;
        }
        throw new RuntimeException("No authenticated user");
    }

    public static String getCurrentUsername() {
        return getCurrentUser().getUsername();
    }

    public static String getCurrentRole() {
        return getCurrentUser().getAuthorities()
                .iterator().next().getAuthority();
    }

    /** Trả về id dù là Artist hay CustomUserDetails */
    public static Integer getCurrentId() {
        UserDetails userDetails = getCurrentUser();
        if (userDetails instanceof Artist artist)           return artist.getId();
        if (userDetails instanceof User user)   return user.getId();
        throw new RuntimeException("Unknown user type");
    }

    public static boolean isArtist() {
        return getCurrentUser() instanceof Artist;
    }
}