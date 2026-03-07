package movieapp.dto.Room;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomBrowseResponse {
    private String code;
    private String roomName;
    private String hostUsername;
    private String hostAvatarUrl;
    private String movieName;
    private String moviePosterUrl;
    private String state; // WAITING, PLAYING
    private int memberCount;
    private int maxMembers;
    private boolean requireApproval;
}
