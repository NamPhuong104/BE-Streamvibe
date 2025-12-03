package movieapp.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "watch_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_movie_episode",
                        columnNames = {"user_id", "movie_slug", "episode_slug"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "movie_slug")
    private String movieSlug;

    @Column(name = "movie_name")
    private String movieName;

    @Column(name = "movie_type")
    private String movieType;

    @Column(name = "episode_slug")
    private String episodeSlug;

    @Column(name = "episode_name")
    private String episodeName;

    @Column(name = "server_name")
    private String serverName;

    @Column(name = "watch_time", nullable = false)
    private Long currentTime;

    @Column(name = "duration", nullable = false)
    private Long duration;

    @Column(name = "progress_percent")
    private Double progressPercent;

    @Column(name = "completed", nullable = false)
    private Boolean completed;

    @Column(name = "last_watched_at", nullable = false)
    private LocalDateTime lastWatchedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastWatchedAt = LocalDateTime.now();
        if (this.completed == null) this.completed = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastWatchedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

}
