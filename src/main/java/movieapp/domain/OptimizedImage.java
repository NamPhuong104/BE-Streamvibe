package movieapp.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "optimized_images")
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

    @Column(nullable = false)
    private String imageType;

    @Column(nullable = false)
    private String cloudinaryPublicId;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
