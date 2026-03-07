package movieapp.dto.Room;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomMemberDTO {
    private Long userId;
    private String userName;
    private String avatarUrl;

    @JsonProperty("isHost")
    private Boolean isHost;
}
