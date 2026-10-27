package vn.edu.fpt.mss.engagementservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** userId/trackId/artistId là id thuần của service khác (user/content), KHÔNG phải quan hệ JPA. */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Người nhận. */
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "artist_id")
    private Integer artistId;

    @Column(name = "track_id")
    private Integer trackId;

    @Column(name = "album_id")
    private Integer albumId;

    /** Lưu sẵn tên (denormalize) vì tên bài/album/nghệ sĩ nằm ở content-service. */
    @Column(name = "track_name", length = 255)
    private String trackName;

    @Column(name = "album_name", length = 255)
    private String albumName;

    @Column(name = "artist_name", length = 255)
    private String artistName;

    @Column(name = "content", length = 500, nullable = false)
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
