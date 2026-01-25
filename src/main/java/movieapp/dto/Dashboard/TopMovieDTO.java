package movieapp.dto.Dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopMovieDTO {
    private Integer rank;
    private String movieSlug;
    private String movieName;
    private String originName;
    private String posterUrl;
    private String optimizedPosterUrl;
    private String thumbUrl;
    private String optimizedThumbUrl;
    private Long viewCount;
    private Long totalWatchTime;
}
