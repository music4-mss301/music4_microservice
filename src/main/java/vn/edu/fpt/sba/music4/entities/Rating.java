package vn.edu.fpt.sba.music4.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "RATINGS", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "track_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(name = "rating_value", nullable = false)
    private Integer ratingValue;
}