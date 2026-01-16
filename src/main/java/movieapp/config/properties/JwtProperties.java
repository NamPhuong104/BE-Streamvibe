package movieapp.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String base64Secret;

    /**
     * Access Token validity: mặc định 1 ngày (86400 seconds)
     */
    private long accessTokenValidityInSeconds = 86400;

    /**
     * Refresh Token validity: mặc định 30 ngày (2592000 seconds)
     */
    private long refreshTokenValidityInSeconds = 2592000;

    /**
     * Lấy access token validity tính bằng milliseconds
     */
    public long getAccessTokenValidityInMs() {
        return accessTokenValidityInSeconds * 1000;
    }

    /**
     * Lấy refresh token validity tính bằng milliseconds
     */
    public long getRefreshTokenValidityInMs() {
        return refreshTokenValidityInSeconds * 1000;
    }
}
