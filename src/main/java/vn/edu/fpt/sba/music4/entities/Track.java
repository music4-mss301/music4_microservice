package vn.edu.fpt.sba.music4.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "TRACKS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String img;

    @Column(length = 10)
    private String duration;

    @Column(name = "file_path", columnDefinition = "NVARCHAR(MAX)")
    private String filePath;

    @Column(name = "preview_path", columnDefinition = "NVARCHAR(MAX)")
    private String previewPath;

    @Column(name = "upload_date", nullable = false)
    private LocalDate uploadDate;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private Album album;

    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TrackArtist> trackArtists;

    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TrackCategory> trackCategories;
}