package movieapp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "watch_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_movie_episode",
                        columnNames = {"user_id", "movie_slug", "episode_slug"}
                )
        },
        indexes = {
                // 1. Composite Index cho phân trang lịch sử xem
                // Query: findByUserIdOrderByLastWatchedAtDesc
                @Index(name = "idx_watch_history_user_watched", columnList = "user_id, last_watched_at DESC"),

                // 2. Composite Index cho query theo movie
                // Query: findLastedByUserAndMovie, existsByUserIdAndMovieSlug
                @Index(name = "idx_watch_history_user_movie", columnList = "user_id, movie_slug, last_watched_at DESC"),

                // 3. Composite Index cho "Tiếp tục xem"
                // Query: findByUserIdAndCompletedFalseOrderByLastWatchedAtDesc
                @Index(name = "idx_watch_history_user_completed", columnList = "user_id, completed, last_watched_at DESC")
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

    @Column(name = "origin_name")
    private String originName;

    @Column(name = "movie_type")
    private String movieType;

    @Column(name = "episode_slug")
    private String episodeSlug;

    @Column(name = "episode_name")
    private String episodeName;

    @Column(name = "server_name")
    private String serverName;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(name = "thumb_url")
    private String thumbUrl;

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
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.lastWatchedAt = now;
        if (this.completed == null) this.completed = false;
    }

    @PreUpdate
    protected void onUpdate() {
        LocalDateTime now = LocalDateTime.now();
        this.lastWatchedAt = now;
        updatedAt = now;
    }

}
