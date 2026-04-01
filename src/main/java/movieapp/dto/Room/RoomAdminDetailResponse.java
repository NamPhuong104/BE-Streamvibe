package movieapp.dto.Room;

import lombok.Builder;
import lombok.Data;
import movieapp.dto.Room.Chat.ChatMessageDTO;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class RoomAdminDetailResponse {
    private String code;
    private String roomName;
    private RoomResponse.RoomHostDTO host;
    private RoomResponse.RoomMovieDTO movie;
    private String state;
    private double currentTime;
    private double speed;

    // Members
    private int memberCount;
    private int maxMembers;
    private boolean requireApproval;
    private List<RoomMemberDTO> members;

    // Pending requests
    private int pendingCount;
    private List<JoinRequestDTO> pendingRequests;

    // Chat
    private long chatMessageCount;
    private List<ChatMessageDTO> chatHistory;

    // Status
    private long durationMinutes;
    private boolean hostDisconnected;
    private Instant createdAt;
}
