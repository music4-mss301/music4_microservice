package vn.edu.fpt.mss.engagementservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** userId/trackId/artistId là id thuần của service khác (user/content), KHÔNG phải quan hệ JPA. */
@Entity
@Table(name = "playlist_tracks", uniqueConstraints = @UniqueConstraint(columnNames = {"playlist_id", "track_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaylistTrack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @Column(name = "track_id", nullable = false)
    private Integer trackId;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;
}
