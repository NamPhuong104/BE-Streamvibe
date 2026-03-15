package movieapp.dto.Room;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JoinRequestDTO {
    private Long userId;
    private String fullName;
    private String username;
    private String avatarUrl;
    private long requestedAt;
}
