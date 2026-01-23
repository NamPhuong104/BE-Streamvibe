package movieapp.dto.Cache;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class CacheActionResponse {
    private String action;
    private String target;
    private Long durationMs;
    private Date timestamp;

    // Affected items
    private Integer affectedCount;
    private List<String> affectedKeys;

    // Errors (nếu có)
    private List<ErrorDetail> errors;

    @Data
    @Builder
    public static class ErrorDetail {
        private String key;
        private String error;
    }
}
