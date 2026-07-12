package vn.edu.fpt.sba.music4.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.edu.fpt.sba.music4.util.JwtUtil;
import vn.edu.fpt.sba.music4.services.impl.ArtistServiceImpl;
import vn.edu.fpt.sba.music4.services.impl.CustomUserDetailsServiceImpl;
import vn.edu.fpt.sba.music4.services.impl.UserServiceImpl;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsServiceImpl compositeUds;
    private final ArtistServiceImpl artistDetailsService;
    private final UserServiceImpl userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/notifications/subscribe/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res); return;
        }

        String token    = header.substring(7);
        String username = jwtUtil.extractUsername(token);
        String source   = jwtUtil.extractSource(token);


        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails user = "artist".equals(source)
                    ? artistDetailsService.loadUserByUsername(username)
                    : userDetailsService.loadUserByUsername(username);
            System.out.println("Load Ok: " + user.getUsername() + " - " + user.getAuthorities());

            if (jwtUtil.isValid(token, user)) {
                var auth = new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(req, res);
    }
}