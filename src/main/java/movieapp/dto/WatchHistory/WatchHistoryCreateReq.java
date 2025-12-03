package movieapp.dto.WatchHistory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WatchHistoryCreateReq {
    @NotNull(message = "userId không được để trống")
    private Long userId;

    @NotBlank(message = "movieSlug không được để trống")
    private String movieSlug;

    private String movieName;
    private String movieThumb;
    private String movieType;

    private String episodeSlug;
    private String episodeName;
    private String serverName;

    @NotNull(message = "currentTime không được để trống")
    @Min(value = 0, message = "currentTime phải >= 0")
    private Long currentTime;

    @NotNull(message = "duration không được để trống")
    @Min(value = 1, message = "duration phải > 0")
    private Long duration;
}
