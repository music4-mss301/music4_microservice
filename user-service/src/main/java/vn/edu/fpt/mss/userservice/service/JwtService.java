package vn.edu.fpt.mss.userservice.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.fpt.mss.userservice.config.JwtProperties;
import vn.edu.fpt.mss.userservice.entity.User;

import javax.crypto.SecretKey;
import java.util.Date;

/** Chỉ user-service PHÁT HÀNH token. Gateway verify bằng cùng secret. */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties props;

    public String generateAccessToken(User user) {
        Date now = new Date();
        var builder = Jwts.builder()
                .subject(user.getUserName())
                .claim("id", user.getId())
                .claim("name", user.getName())
                .claim("role", user.getRole())
                .claim("source", "ARTIST".equals(user.getRole()) ? "artist" : "user")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + props.accessExpirationMs()));
        if (user.getArtistId() != null) {
            builder.claim("artistId", user.getArtistId());
        }
        return builder.signWith(key()).compact();
    }

    public long accessExpirationSeconds() {
        return props.accessExpirationMs() / 1000;
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.secret()));
    }
}
