package movieapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(nullable = false, unique = true)
    private String originalUrl;

    @Column(nullable = false)
    private String cloudinaryUrl;

    @Column(name = "image_type", nullable = false)
    private String imageType;

    @Column(nullable = false)
    private String cloudinaryPublicId;

    @Column(nullable = false)
    private String slug;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
