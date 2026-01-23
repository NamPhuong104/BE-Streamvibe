package movieapp.dto.Cache;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Builder
public class CacheListResponse {
    // Data
    private List<CacheItemDTO> items;

    // Pagination
    private PageMeta meta;

    // Filter info
    private FilterInfo filter;

    @Data
    @Builder
    public static class PageMeta {
        private Integer page;
        private Integer size;
        private Integer totalItems;
        private Integer totalPages;
        private Boolean hasNext;
        private Boolean hasPrevious;
    }

    @Data
    @Builder
    public static class FilterInfo {
        private String group;
        private String status;
        private String search;
    }
}
