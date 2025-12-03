package movieapp.dto.MetaAndHead;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Params {
    @JsonProperty("type_slug")
    private String typeSlug;

    private Pagination pagination;

    @Data
    public static class Pagination {
        private Integer totalItems;
        private Integer totalItemsPerPage;
        private Integer currentPage;
        private Integer pageRanges;
    }
}
