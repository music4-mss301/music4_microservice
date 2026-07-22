package vn.edu.fpt.sba.music4.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

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

    @Column(nullable = false, columnDefinition = "NVARCHAR(100)",length = 100)
    private String name;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String img;

    @Column(nullable = false)
    private Integer duration;

    @Column(name = "file_path", columnDefinition = "NVARCHAR(MAX)")
    private String filePath;

    @Column(name = "preview_path", columnDefinition = "NVARCHAR(MAX)")
    private String previewPath;

    @Column(name = "upload_date", nullable = false, updatable = false)
    private LocalDate uploadDate;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id", nullable = true)
    @JsonIgnore
    private Album album;

    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnore
    private List<TrackArtist> trackArtists;

    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<TrackCategory> trackCategories;


    @PrePersist
    protected void onCreate() {
        this.uploadDate = LocalDate.now();
        if (this.viewCount == null) {
            this.viewCount = 0; // Đảm bảo chắc chắn không bị null trước khi vào DB
        }
    }
}