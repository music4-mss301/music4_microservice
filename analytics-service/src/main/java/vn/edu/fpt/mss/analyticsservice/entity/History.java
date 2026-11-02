package vn.edu.fpt.mss.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Lịch sử nghe. userId null = khách chưa đăng nhập. trackId là id thuần của content-service. */
@Entity
@Table(name = "history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class History {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "track_id", nullable = false)
    private Integer trackId;

    @CreationTimestamp
    @Column(name = "listened_at", nullable = false, updatable = false)
    private LocalDateTime listenedAt;

    /** Vị trí phát (giây) để nghe tiếp. */
    @Builder.Default
    @Column(name = "playback_position", nullable = false)
    private int playbackPosition = 0;
}
