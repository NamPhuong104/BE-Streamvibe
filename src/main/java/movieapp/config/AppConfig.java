package movieapp.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.config.properties.*;
import org.springframework.stereotype.Component;

@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class AppConfig {
    private final AppProperties app;
    private final OPhimProperties ophim;
    private final JwtProperties jwt;
    private final GoogleProperties google;
    private final CacheProperties cache;
    private final MailProperties mail;

    // ==================== CONVENIENCE METHODS ====================

    /**
     * Lấy full image URL từ Ophim
     */
    public String getOphimImageUrl(String path) {
        return ophim.getFullImageUrl(path);
    }

    /**
     * Lấy JWT access token validity (ms)
     */
    public long getAccessTokenValidityMs() {
        return jwt.getAccessTokenValidityInMs();
    }

    /**
     * Lấy JWT refresh token validity (ms)
     */
    public long getRefreshTokenValidityMs() {
        return jwt.getRefreshTokenValidityInMs();
    }
}
