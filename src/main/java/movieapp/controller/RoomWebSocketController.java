package movieapp.controller;

import lombok.RequiredArgsConstructor;
import movieapp.dto.Room.Chat.ChatDeleteDTO;
import movieapp.dto.Room.Chat.ChatSendDTO;
import movieapp.dto.Room.MovieSuggestDTO;
import movieapp.dto.Room.RoomStateDTO;
import movieapp.service.RoomChatService;
import movieapp.service.RoomService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class RoomWebSocketController {
    private final RoomService roomService;
    private final RoomChatService roomChatService;

    @MessageMapping("/room/{code}/sync")
    public void asyncState(@DestinationVariable String code, @Payload RoomStateDTO state, Principal principal) {
        roomService.syncState(code, state, principal);
    }

    @MessageMapping("/room/{code}/suggest")
    public void suggestMovie(@DestinationVariable String code, @Payload MovieSuggestDTO suggestion, Principal principal) {
        roomService.suggestMovie(code, suggestion, principal);
    }

    /**
     * Client gửi tin nhắn
     * <p>
     * Client publish to: /app/room/{code}/chat/send
     * Server broadcast to: /topic/room/{code}/chat
     */
    @MessageMapping("/room/{code}/chat/send")
    public void sendChatMessage(@DestinationVariable String code, @Payload ChatSendDTO dto, Principal principal) {
        roomChatService.sendMessage(code, dto, principal);
    }

    /**
     * Client xóa tin nhắn
     * <p>
     * Client publish to: /app/room/{code}/chat/delete
     * Server broadcast to: /topic/room/{code}/chat/delete
     */
    @MessageMapping("/room/{code}/chat/delete")
    public void deleteChatMessage(@DestinationVariable String code, @Payload ChatDeleteDTO dto, Principal principal) {
        roomChatService.deleteMessage(code, dto, principal);
    }

    /**
     * Client đang gõ
     * <p>
     * Client publish to: /app/room/{code}/chat/typing
     * Server broadcast to: /topic/room/{code}/chat/typing
     * <p>
     * Không cần @Payload vì chỉ cần biết AI đang typing
     * Server sẽ lấy user info từ Principal
     */
    @MessageMapping("/room/{code}/chat/typing")
    public void typing(@DestinationVariable String code, Principal principal) {
        roomChatService.handleTyping(code, principal);
    }
}
