package movieapp.dto.Room.Chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageDTO {
    private String id;

    @Builder.Default
    private String type = "USER";

    private Long senderId;
    private String senderUsername;
    private String senderFullName;
    private String senderAvatarUrl;

    private String content;

    private String replyToId;
    private String replyToContent;
    private String replyToSenderName;

    private Double videoTimestamp;
    private Long createdAt;

    @Builder.Default
    private Boolean deleted = false;
}
