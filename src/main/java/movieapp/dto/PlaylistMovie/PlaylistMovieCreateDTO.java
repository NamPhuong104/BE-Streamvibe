package movieapp.dto.PlaylistMovie;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaylistMovieCreateDTO {
    @NotNull(message = "PlaylistId không được để trống")
    private Long playlistId;

    @NotBlank(message = "MovieSlug không được để trống")
    private String movieSlug;
    private String movieName;
    private String originName;
    private String posterUrl;
    private String thumbUrl;
    private String quality;
    private String lang;
    private String episodeCurrent;
}
