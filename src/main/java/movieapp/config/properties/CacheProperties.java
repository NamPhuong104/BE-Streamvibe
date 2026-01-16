package movieapp.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {
    /**
     * TTL cho cache (phút)
     */
    private int ttlMinutes = 60;

    /**
     * Delay trước khi warmup cache (giây)
     */
    private int warmupDelaySeconds = 10;

    /**
     * Số threads để fetch detail
     */
    private int detailFetchThreads = 10;

    /**
     * Timeout cho API calls (giây)
     */
    private int apiTimeoutSeconds = 10;

    /**
     * Schedule config
     */
    private ScheduleConfig schedule = new ScheduleConfig();

    @Data
    public static class ScheduleConfig {
        private boolean enabled = true;
        private String initialGroup = "1 0 * * * *";
        private String group1 = "0 55 * * * *";
        private String group2 = "0 56 * * * *";
    }

    /**
     * Lấy TTL tính bằng milliseconds
     */
    public long getTtlMs() {
        return ttlMinutes * 60 * 1000L;
    }

    /**
     * Lấy API timeout tính bằng milliseconds
     */
    public long getApiTimeoutMs() {
        return apiTimeoutSeconds * 1000L;
    }
}
