package movieapp.dto.Room;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class RoomStatsResponse {
    private int totalActiveRooms;
    private int totalOnlineUsers;
    private double avgMembersPerRoom;
    private int roomsPlaying;
    private int roomsWaiting;
    private List<RoomSummary> rooms;

    @Data
    @Builder
    public static class RoomSummary {
        private String code;
        private String roomName;
        private String hostUsername;
        private String hostFullName;
        private String hostAvatarUrl;
        private String movieName;
        private String movieSlug;
        private String moviePosterUrl;
        private String state;
        private int memberCount;
        private int maxMembers;
        private int pendingCount;
        private boolean requireApproval;
        private long chatMessageCount;
        private long durationMinutes;
        private boolean hostDisconnected;
        private Instant createdAt;
    }
}
