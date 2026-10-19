package vn.edu.fpt.mss.mediaservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "media_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object_key", length = 500, nullable = false, unique = true)
    private String objectKey;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 255)
    private String contentType;

    /** IMAGE | AUDIO */
    @Column(name = "media_type", length = 50, nullable = false)
    private String mediaType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "bucket_name", length = 255, nullable = false)
    private String bucketName;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "uploaded_by")
    private Integer uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
