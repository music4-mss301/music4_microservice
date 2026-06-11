package vn.edu.fpt.sba.music4.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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

    @Column(nullable = false,columnDefinition = "NVARCHAR(100)", length = 100)
    private String name;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String img;

    @Column(name = "upload_date", nullable = false)
    private LocalDate uploadDate;

    @Column(name = "track_total", nullable = false)
    private Integer trackTotal = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "albums"})
    private Artist artist;

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "album"})
    private List<Track> tracks;

    @PrePersist
    protected void onCreate() {
        this.uploadDate = LocalDate.now();
    }
}