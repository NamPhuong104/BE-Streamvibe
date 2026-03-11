package movieapp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.service.RoomService;
import movieapp.service.WsUserCacheService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {
    private final RoomService roomService;
    private final WsUserCacheService wsUserCacheService;

    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = accessor.getSessionId();

        if (accessor.getUser() instanceof JwtAuthenticationToken auth) {
            Jwt jwt = auth.getToken();
            String email = jwt.getSubject();
            log.info("WebSocket connected: email={}, session={}", email, sessionId);
            roomService.handleUserReconnect(email);
        }
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        if (accessor.getUser() instanceof JwtAuthenticationToken auth) {
            Jwt jwt = auth.getToken();
            String email = jwt.getSubject();
            log.info("WebSocket disconnected: email={}, session={}", email, sessionId);

            roomService.handleUserDisconnect(email);
            wsUserCacheService.evict(email);
        }
    }
}
