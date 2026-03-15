package movieapp.dto.Room.Chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatTypingDTO {
    private Long userId;
    private String username;
    private String fullName;
    private boolean isTyping;
}
