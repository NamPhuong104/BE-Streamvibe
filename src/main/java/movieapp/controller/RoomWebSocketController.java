package movieapp.controller;

import lombok.RequiredArgsConstructor;
import movieapp.dto.Room.MovieSuggestDTO;
import movieapp.dto.Room.RoomStateDTO;
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

    @MessageMapping("/room/{code}/sync")
    public void asyncState(@DestinationVariable String code, @Payload RoomStateDTO state, Principal principal) {
        roomService.syncState(code, state, principal);
    }

    @MessageMapping("/room/{code}/suggest")
    public void suggestMovie(@DestinationVariable String code, @Payload MovieSuggestDTO suggestion, Principal principal) {
        roomService.suggestMovie(code, suggestion, principal);
    }
}
