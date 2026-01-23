package movieapp.dto.Cache;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
@Builder
public class CacheStatsDTO {
    private Integer totalKeys;
    private Integer activeKeys;
    private Integer expiredKeys;
    private Long totalSizeBytes;
    private String totalSizeFormatted;

    private Map<String, GroupStats> groupStats;

    private Date lastRefreshTime;
    private Date nextScheduledRefresh;
    private Long uptimeSeconds;

    private CacheConfig config;

    @Data
    @Builder
    public static class GroupStats {
        private String group;
        private Integer totalSections;
        private Integer cachedSections;
        private Integer expiredSections;
        private Long avgTtlSeconds;
    }

    @Data
    @Builder
    public static class CacheConfig {
        private Integer ttlMinutes;
        private Integer warmUpDelaySeconds;
        private String refreshSchedule;
    }
}
