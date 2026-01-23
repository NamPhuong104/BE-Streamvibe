package movieapp.dto.Cache;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class CacheItemDTO {
    private String key;
    private String section;
    private String group;
    private String status;
    private Long ttlSeconds;
    private Long ttlMinutes;
    private Date expiresAt;
    private Date cachedAt;
    private Long sizeBytes;
    private String sizeFormatted;
    private Integer itemCount;
    private String type;
}
