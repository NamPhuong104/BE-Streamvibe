package movieapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roles")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    /**
     * Thứ tự ưu tiên (số nhỏ = quyền cao hơn)
     * SUPER_ADMIN = 0, ADMIN = 10, MODERATOR = 50, PREMIUM = 80, USER = 100
     */
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 100;

    @Column(name = "is_system_role")
    @Builder.Default
    private Boolean isSystemRole = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<User> users = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Check role này có quyền cao hơn hoặc bằng role khác không
     */
    public boolean hasPrivilegeOver(Role other) {
        if (other == null) return true;
        return this.priority <= other.priority;
    }

    /**
     * Check role này có quyền cao hơn hoặc bằng priority level
     */
    public boolean hasPrivilegeOver(int priorityLevel) {
        return this.priority <= priorityLevel;
    }

    /**
     * Check có phải admin role không (priority <= 10)
     */
    public boolean isAdminRole() {
        return this.priority != null && this.priority <= 10;
    }

    /**
     * Check có phải moderator trở lên không (priority <= 50)
     */
    public boolean isModeratorOrAbove() {
        return this.priority != null && this.priority <= 50;
    }

    /**
     * Check có phải premium trở lên không (priority <= 80)
     */
    public boolean isPremiumOrAbove() {
        return this.priority != null && this.priority <= 80;
    }
}
