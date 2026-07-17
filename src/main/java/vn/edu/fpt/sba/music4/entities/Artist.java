package vn.edu.fpt.sba.music4.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "ARTISTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "is_deleted = 0")
public class Artist implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String img;

    @Column(nullable = false, columnDefinition = "NVARCHAR(100)",length = 100)
    private String name;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "user_name", nullable = false, length = 50, unique = true)
    private String userName;

    @Column(name = "track_total", nullable = false)
    private Integer trackTotal;

//    @Column(name = "gender")
//    private Boolean gender;
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String cover;

    @Column(name = "album_total", nullable = false)
    private Integer albumTotal;

    @OneToMany(mappedBy = "artist", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // <-- BỔ SUNG DÒNG NÀY
    private List<Album> albums;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_ARTIST"));
    }
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
    @Override
    public String getUsername() {
        return this.userName;
    }
}