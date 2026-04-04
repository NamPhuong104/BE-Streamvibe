package movieapp.dto.Room;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomCurrentTimeResponse {
    private double currentTime;
    private String state;
    private double speed;
    private String episode;
    private String videoUrl;
    private long timestamp;
}
