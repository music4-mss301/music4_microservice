package vn.edu.fpt.sba.music4.entities;

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
    private Track track;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
}