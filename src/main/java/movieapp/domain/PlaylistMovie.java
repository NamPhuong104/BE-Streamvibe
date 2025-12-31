package movieapp.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "playlist_movies",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_playlist_movie",
                        columnNames = {"playlist_id", "movie_slug"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaylistMovie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

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

    @Column(name = "quality")
    private String quality;

    @Column(name = "lang")
    private String lang;

    @Column(name = "episode_current")
    private String episodeCurrent;

    @Column(name = "added_at")
    private LocalDateTime addedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.addedAt = now;
    }
}
