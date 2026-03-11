package movieapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "optimized_images", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_slug_image_type",
                columnNames = {"slug", "image_type"}
        ),
},
        indexes = {
                // Index cho slug (không unique vì có thể cùng slug khác image_type)
                // Nhưng uk_slug_image_type đã cover các query theo slug
                // Nếu có query chỉ theo slug thì mới cần index này
                @Index(name = "idx_optimized_image_slug", columnList = "slug")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizedImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "original_url", nullable = false, unique = true, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "optimized_url", nullable = false, columnDefinition = "TEXT")
    private String optimizedUrl;

    @Column(name = "image_type", nullable = false)
    private String imageType;

    @Column(nullable = false)
    private String slug;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
