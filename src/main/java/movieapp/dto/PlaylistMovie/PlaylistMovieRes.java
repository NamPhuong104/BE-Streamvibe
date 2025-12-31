package movieapp.dto.PlaylistMovie;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaylistMovieRes {
    private Long id;
    private String movieSlug;
    private String movieName;
    private String originName;
    private String posterUrl;
    private String thumbUrl;
    private String quality;
    private String lang;
    private String episodeCurrent;
    private LocalDateTime addedAt;
    private LocalDateTime createdAt;

    private String optimizedPoster;
    private String optimizedThumb;

    private PlaylistInfo playlist;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaylistInfo {
        private Long id;
        private String name;
    }
}
