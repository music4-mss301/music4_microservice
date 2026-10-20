package vn.edu.fpt.mss.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "img", columnDefinition = "TEXT")
    private String img;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    /** Luôn là chuỗi BCrypt, không bao giờ lưu plaintext. */
    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "user_name", length = 50, nullable = false, unique = true)
    private String userName;

    /** LISTENER | ARTIST | ADMIN */
    @Builder.Default
    @Column(name = "role", length = 20, nullable = false)
    private String role = "LISTENER";

    /** Id hồ sơ nghệ sĩ bên content-service (chỉ có khi role = ARTIST). */
    @Column(name = "artist_id")
    private Integer artistId;

    @Column(name = "gender")
    private Boolean gender;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
