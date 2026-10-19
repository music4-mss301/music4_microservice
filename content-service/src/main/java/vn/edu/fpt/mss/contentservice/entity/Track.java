package vn.edu.fpt.mss.contentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tracks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Track {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "img", columnDefinition = "NVARCHAR(MAX)")
    private String img;

    @Column(name = "duration")
    private Integer duration;

    /** Object key trên S3 (media-service quản lý file). */
    @Column(name = "file_path", columnDefinition = "NVARCHAR(MAX)")
    private String filePath;

    @Column(name = "preview_path", columnDefinition = "NVARCHAR(MAX)")
    private String previewPath;

    @CreationTimestamp
    @Column(name = "upload_date", nullable = false, updatable = false)
    private LocalDate uploadDate;

    @Builder.Default
    @Column(name = "view_count", nullable = false)
    private long viewCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private Album album;

    @Builder.Default
    @OrderBy("id ASC")
    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrackArtist> trackArtists = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "track", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrackCategory> trackCategories = new ArrayList<>();
}
