package movieapp.dto.WatchHistory;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WatchHistoryUpdateReq {
    @NotNull(message = "id không được để trống")
    private Long id;

    private Long userId;
    private String movieSlug;
    private String movieName;
    private String movieType;
    private String episodeSlug;
    private String episodeName;
    private String serverName;
    private Long currentTime;
    private Long duration;
}
