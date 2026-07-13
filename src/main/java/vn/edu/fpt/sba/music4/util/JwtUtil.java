package vn.edu.fpt.sba.music4.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import vn.edu.fpt.sba.music4.entities.Artist;
import vn.edu.fpt.sba.music4.entities.User;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtUtil {
    @Value("${JWT_SECRET_KEY}") private String secret;
    private final long expMs = 1000L * 60 * 60 * 24 * 7;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateToken(UserDetails user) {
        // xác định source để filter sau này biết load từ bảng nào
        String source = (user instanceof Artist) ? "artist" : "user";
        String role   = user.getAuthorities().iterator().next().getAuthority();
        if(user instanceof Artist) {
            Artist castArtist = (Artist) user;
            return Jwts.builder()
                    .subject(user.getUsername())
                    .claim("id", castArtist.getId())
                    .claim("img", castArtist.getImg())
                    .claim("role",   role)
                    .claim("source", source)
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + expMs))
                    .signWith(key())
                    .compact();
        }

        User castUser = (User) user;

        System.out.println("Role: " + role);
        System.out.println("Username: " + user.getUsername());

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("id", castUser.getId())
                .claim("img", castUser.getImg())
                .claim("role",   role)
                .claim("source", source)   // <-- key point
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expMs))
                .signWith(key())
                .compact();
    }

    public String extractUsername(String token) {
        return claims(token).getSubject();
    }

    public String extractSource(String token) {
        return claims(token).get("source", String.class);
    }
    public boolean isValid(String token, UserDetails user) {
        return extractUsername(token).equals(user.getUsername())
                && !claims(token).getExpiration().before(new Date());
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token).getPayload();
    }


}
