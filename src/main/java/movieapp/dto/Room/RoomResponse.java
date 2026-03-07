package movieapp.dto.Room;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class RoomResponse {
    private String code;
    private String roomName;
    private RoomHostDTO host;
    private RoomMovieDTO movie;
    private String state;
    private int memberCount;
    private int maxMembers;
    private boolean requireApproval;
    private List<RoomMemberDTO> members;
    private int pendingCount;
    private Instant createdAt;

    @Data
    @Builder
    public static class RoomHostDTO {
        private Long id;
        private String username;
        private String avatarUrl;
    }

    @Data
    @Builder
    public static class RoomMovieDTO {
        private String slug;
        private String name;
        private String posterUrl;
        private String thumbUrl;
        private String currentEpisode;
        private int currentServerIndex;
        private String videoUrl;
        private String episodeCurrent;
        private String quality;
        private String lang;
    }
}
