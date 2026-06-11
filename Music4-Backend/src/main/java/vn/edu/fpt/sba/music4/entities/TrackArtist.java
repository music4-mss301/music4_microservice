package vn.edu.fpt.sba.music4.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TRACK_ARTISTS", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"track_id", "artist_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrackArtist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @Column(nullable = false, length = 50)
    private String role; // Main, Featured, Producer
}