package movieapp.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorites", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "movie_slug"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "movie_slug", nullable = false)
    private String movieSlug;

    @Column(name = "movie_name")
    private String movieName;

    @Column(name = "origin_name")
    private String originName;

    @Column(name = "poster_url")
    private String posterUrl;
    
    @Column(name = "thumb_url")
    private String thumbUrl;

    @Column(name = "lang")
    private String lang;

    @Column(name = "quality")
    private String quality;

    @Column(name = "episode_current")
    private String episodeCurrent;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
