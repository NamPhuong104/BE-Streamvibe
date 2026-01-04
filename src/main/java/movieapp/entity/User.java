package movieapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users", indexes = {
        // 1. Refresh Token - dùng cho refresh access token
        // Query: findByRefreshTokenAndEmail
        @Index(name = "idx_user_refresh_token", columnList = "refresh_token"),

        // 2. Reset Password Token - dùng cho reset password flow
        // Query: findByResetPasswordToken
        @Index(name = "idx_user_reset_password_token", columnList = "reset_password_token"),

        // 3. Verify Email Token - dùng cho xác thực email
        // Query: findByVerifyEmailToken
        @Index(name = "idx_user_verify_email_token", columnList = "verify_email_token"),

        // 4. Change Email Token - dùng cho đổi email
        // Query: findByChangeEmailToken
        @Index(name = "idx_user_change_email_token", columnList = "change_email_token")
})
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

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Playlist> playlists;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Lấy danh sách tên roles
     */
    public List<String> getRoleNames() {
        if (roles == null) return List.of();
        return roles.stream().map(Role::getName).collect(Collectors.toList());
    }

    /**
     * Check user có role cụ thể không
     */
    public boolean hasRole(String roleName) {
        if (roles == null) return false;
        return roles.stream().anyMatch(role -> role.getName().equals(roleName));
    }

    /**
     * Check user có một trong các roles không
     */
    public boolean hasAnyRole(String... roleNames) {
        if (roles == null) return false;
        Set<String> targetRoles = Set.of(roleNames);
        return roles.stream().anyMatch(role -> targetRoles.contains(role.getName()));
    }

    /**
     * Check user có phải Admin không (ADMIN hoặc SUPER_ADMIN)
     */
    public boolean isAdmin() {
        return hasAnyRole("ROLE_ADMIN", "ROLE_SUPER_ADMIN");
    }

    /**
     * Check user có phải Super Admin không
     */
    public boolean isSuperAdmin() {
        return hasRole("ROLE_SUPER_ADMIN");
    }

    /**
     * Check user có phải Premium trở lên không
     */
    public boolean isPremium() {
        return hasAnyRole("ROLE_PREMIUM", "ROLE_MODERATOR", "ROLE_ADMIN", "ROLE_SUPER_ADMIN");
    }

    /**
     * Check user có phải Moderator trở lên không
     */
    public boolean isModerator() {
        return hasAnyRole("ROLE_MODERATOR", "ROLE_ADMIN", "ROLE_SUPER_ADMIN");
    }

    /**
     * Lấy role có quyền cao nhất (priority nhỏ nhất)
     */
    public Role getHighestRole() {
        if (roles == null || roles.isEmpty()) return null;
        return roles.stream()
                .min((r1, r2) -> r1.getPriority().compareTo(r2.getPriority()))
                .orElse(null);
    }

    /**
     * Lấy tên role chính (role cao nhất)
     */
    public String getPrimaryRoleName() {
        Role highest = getHighestRole();
        return highest != null ? highest.getName() : "ROLE_USER";
    }
}
