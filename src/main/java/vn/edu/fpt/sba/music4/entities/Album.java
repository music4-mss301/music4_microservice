package vn.edu.fpt.sba.music4.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "ALBUMS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String img;

    @Column(name = "upload_date", nullable = false)
    private LocalDate uploadDate;

    @Column(name = "track_total", nullable = false)
    private Integer trackTotal = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Track> tracks;

    @PrePersist
    protected void onCreate() {
        this.uploadDate = LocalDate.now();
    }
}