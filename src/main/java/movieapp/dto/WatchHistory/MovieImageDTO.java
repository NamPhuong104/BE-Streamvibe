package movieapp.dto.WatchHistory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieImageDTO {
    private String thumbUrl;
    private String posterUrl;
    private String optimizedThumb;
    private String optimizedPoster;
}
