package movieapp.dto.Room;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomStateDTO {
    private String action;  // PLAY, PAUSE, SEEK, EPISODE_CHANGE
    private double currentTime;
    private String videoUrl;
    private String episodeSlug;
    private int serverIndex;
    private double speed;
    private long timestamp; // for sync accuracy
}
