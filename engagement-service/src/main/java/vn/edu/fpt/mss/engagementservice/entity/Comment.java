package vn.edu.fpt.mss.engagementservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** userId/trackId/artistId là id thuần của service khác (user/content), KHÔNG phải quan hệ JPA. */
@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "track_id", nullable = false)
    private Integer trackId;

    /** Mọi tài khoản (kể cả nghệ sĩ) đều nằm ở user-service nên chỉ cần userId. */
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "content", length = 500, nullable = false)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
