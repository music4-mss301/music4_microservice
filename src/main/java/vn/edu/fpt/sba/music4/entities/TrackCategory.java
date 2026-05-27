package vn.edu.fpt.sba.music4.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TRACK_CATEGORIES", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"track_id", "category_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrackCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "trackArtists", "trackCategories", "album"})
    private Track track;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "trackCategories"})
    private Category category;
}