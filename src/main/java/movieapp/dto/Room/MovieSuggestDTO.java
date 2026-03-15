package movieapp.dto.Room;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MovieSuggestDTO {
    private String movieSlug;
    private String movieName;
    private String posterUrl;
    private String thumbUrl;
    private Long suggestedByUserId;
    private String suggestedByFullName;
    private String suggestedByUsername;
    private long suggestedAt;
}
