package movieapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import movieapp.util.constant.RoleEnum;

import java.time.LocalDateTime;
import java.util.List;

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
        @Index(name = "idx_user_change_email_token", columnList = "change_email_token"),

        // 5. Find RoleId - dùng cho tìm role
        // Query: findByRoleId
        @Index(name = "idx_user_role_id", columnList = "role_id")
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    @JsonIgnoreProperties({"users", "hibernateLazyInitializer", "handler"})
    private Role role;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== HELPER METHODS (ĐƠN GIẢN HÓA) ====================

    /**
     * Lấy tên role
     */
    public String getRoleName() {
        return role != null ? role.getName() : RoleEnum.ROLE_USER.getName();
    }

    /**
     * Lấy priority của role
     */
    public Integer getRolePriority() {
        return role != null ? role.getPriority() : RoleEnum.ROLE_USER.getPriority();
    }

    /**
     * Check user có role cụ thể không
     */
    public boolean hasRole(String roleName) {
        return role != null && role.getName().equals(roleName);
    }

    /**
     * Check user có role với priority <= level không
     * (priority thấp = quyền cao)
     */
    public boolean hasMinimumPriority(int priorityLevel) {
        return role != null && role.getPriority() <= priorityLevel;
    }

    /**
     * Check user có phải Admin không (ADMIN hoặc SUPER_ADMIN, priority <= 10)
     */
    public boolean isAdmin() {
        return hasMinimumPriority(RoleEnum.ROLE_ADMIN.getPriority());
    }

    /**
     * Check user có phải Super Admin không
     */
    public boolean isSuperAdmin() {
        return hasRole(RoleEnum.ROLE_SUPER_ADMIN.getName());
    }

    /**
     * Check user có phải Moderator trở lên không (priority <= 50)
     */
    public boolean isModerator() {
        return hasMinimumPriority(RoleEnum.ROLE_MODERATOR.getPriority());
    }

    /**
     * Check user có phải Premium trở lên không (priority <= 80)
     */
    public boolean isPremium() {
        return hasMinimumPriority(RoleEnum.ROLE_PREMIUM.getPriority());
    }

    /**
     * Check user có quyền cao hơn user khác không
     */
    public boolean hasPrivilegeOver(User other) {
        if (other == null || other.getRole() == null) return true;
        if (this.role == null) return false;
        return this.role.getPriority() < other.getRole().getPriority();
    }

    /**
     * Check user có quyền cao hơn hoặc bằng user khác không
     */
    public boolean hasPrivilegeOverOrEqual(User other) {
        if (other == null || other.getRole() == null) return true;
        if (this.role == null) return false;
        return this.role.getPriority() <= other.getRole().getPriority();
    }

    /**
     * Check user có quyền quản lý role này không
     * (Chỉ có thể quản lý roles có priority cao hơn - số lớn hơn)
     */
    public boolean canManageRole(Role targetRole) {
        if (targetRole == null) return true;
        if (this.role == null) return false;
        return this.role.getPriority() < targetRole.getPriority();
    }
}
