package movieapp.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    @Column(nullable = false, unique = true)
    private String username;

    private String fullName;
    private String avatarUrl;

    @Builder.Default
    private String provider = "LOCAL";
    private String providerId;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean isEmailVerified = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonIgnore
    @Column(columnDefinition = "MEDIUMTEXT")
    private String refreshToken;

    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @Column(name = "reset_password_expiry")
    private LocalDateTime resetPasswordExpiry;

    @Column(name = "verify_email_token")
    private String verifyEmailToken;

    @Column(name = "verify_email_expiry")
    private LocalDateTime verifyEmailExpiry;

    @Column(name = "pending_email")
    private String pendingEmail;

    @Column(name = "change_email_token")
    private String changeEmailToken;

    @Column(name = "change_email_expiry")
    private LocalDateTime changeEmailExpiry;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Favorite> favorites;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<WatchHistory> watchHistories;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
