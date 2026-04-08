package movieapp.dto.Room.Chat;

import lombok.Data;

import java.util.List;

@Data
public class ChatSendDTO {
    private String content;
    private String replyToId;
    private List<Long> mentions;
}
