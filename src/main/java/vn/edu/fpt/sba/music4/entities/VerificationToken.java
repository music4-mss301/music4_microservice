package vn.edu.fpt.sba.music4.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "VERIFICATION_TOKENS")
public class VerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;
    private String email;
    private String role;
    private LocalDateTime expiryDate;

    public VerificationToken(String token, String email, String role) {
        this.token = token;
        this.email = email;
        this.role = role;
        this.expiryDate = LocalDateTime.now().plusHours(24);
    }
}
