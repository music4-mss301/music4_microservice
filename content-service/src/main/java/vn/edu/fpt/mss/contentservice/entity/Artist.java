package vn.edu.fpt.mss.contentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Hồ sơ nghệ sĩ. KHÔNG chứa email/password: tài khoản đăng nhập nằm ở user-service (user_id). */
@Entity
@Table(name = "artists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")   // unique qua filtered index UX_artists_user_id (cho phép nhiều NULL)
    private Integer userId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "img", columnDefinition = "NVARCHAR(MAX)")
    private String img;

    @Column(name = "cover", columnDefinition = "NVARCHAR(MAX)")
    private String cover;

    @Column(name = "gender")
    private Boolean gender;

    @Builder.Default
    @Column(name = "track_total", nullable = false)
    private int trackTotal = 0;

    @Builder.Default
    @Column(name = "album_total", nullable = false)
    private int albumTotal = 0;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
