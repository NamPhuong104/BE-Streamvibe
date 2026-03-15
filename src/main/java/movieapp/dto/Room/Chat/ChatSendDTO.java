package movieapp.dto.Room.Chat;

import lombok.Data;

@Data
public class ChatSendDTO {
    private String content;
    private String replyToId;
}
