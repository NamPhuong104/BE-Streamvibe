package movieapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.entity.User;
import movieapp.exception.CommonMessageException;
import movieapp.repository.UserRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class WsUserCacheService {
    private final UserRepository userRepository;

    private final Map<String, Long> emailToUserIdCache = new ConcurrentHashMap<>();

    /**
     * Lấy userId từ WebSocket Principal — có cache.
     * Dùng cho các operation chỉ cần userId (syncState, suggest).
     */
    public Long getUserIdFromPrincipal(Principal principal) {
        if (!(principal instanceof JwtAuthenticationToken auth))
            throw new CommonMessageException("Yêu cầu xác thực WebSocket");

        String email = auth.getToken().getSubject();

        Long cacheId = emailToUserIdCache.get(email);
        if (cacheId != null) return cacheId;

        User user = userRepository.findByEmail(email).orElseThrow(() -> new CommonMessageException("User không tồn tại"));

        emailToUserIdCache.put(email, user.getId());
        log.debug("Cached userId {} for email {}", user.getId(), email);

        return user.getId();
    }

    /**
     * Lấy full User entity — KHÔNG cache, dùng khi cần full data.
     */
    public User getFullUser(Principal principal) {
        if (!(principal instanceof JwtAuthenticationToken auth))
            throw new CommonMessageException("Yêu cầu xác thực WebSocket");
        String email = auth.getToken().getSubject();

        return userRepository.findByEmail(email).orElseThrow(() -> new CommonMessageException("User không tồn tại"));
    }

    /**
     * Clear cache khi user disconnect (optional cleanup).
     */
    public void evict(String email) {
        emailToUserIdCache.remove(email);
    }

}
