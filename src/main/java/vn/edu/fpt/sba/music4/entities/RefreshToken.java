package vn.edu.fpt.sba.music4.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Data
@Table(name = "REFRESH_TOKENS")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;           // UUID random

    @Column(nullable = false)
    private String username;          // username hoặc ID — dùng string để chứa cả User lẫn Artist

    @Column(nullable = false)
    private String source;          // "user" | "artist"

    @Column(nullable = false)
    private Instant expiryDate;

    private boolean revoked = false;
}
