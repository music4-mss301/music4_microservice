package vn.edu.fpt.mss.contentservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "track_artists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackArtist {
    public static final String MAIN = "MAIN";
    public static final String FEAT = "FEAT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @Builder.Default
    @Column(name = "role", length = 50, nullable = false)
    private String role = MAIN;
}
