package vn.edu.fpt.sba.music4.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "PASSWORD_RESET_TOKENS")
@Data
public class PasswordResetToken {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    private boolean used = false;

    // Kiểm tra token còn hiệu lực không
    public boolean isValid() {
        return !used && LocalDateTime.now().isBefore(expiryDate);
    }
}
