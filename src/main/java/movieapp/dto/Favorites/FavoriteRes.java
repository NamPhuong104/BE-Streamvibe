package movieapp.dto.Favorites;

import lombok.*;

import java.time.LocalDateTime;

@Data
public class FavoriteRes {
    private Long id;
    private ResUserDTO user;

    private String movieSlug;
    private String movieName;
    private String originName;
    private String posterUrl;
    private String optimizedPoster;
    private String thumbUrl;
    private String optimizedThumb;
    private String episodeCurrent;
    private String lang;
    private String quality;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class ResUserDTO {
        private Long id;
        private String email;
    }
}
