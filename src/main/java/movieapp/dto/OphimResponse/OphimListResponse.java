package movieapp.dto.OphimResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import movieapp.dto.MetaAndHead.Params;
import movieapp.dto.MetaAndHead.SeoOnPage;

import java.util.ArrayList;
import java.util.List;

@Data
public class OphimListResponse {
    private String status;
    private String message;
    private ListData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListData {
        private SeoOnPage seoOnPage;
        private List<OphimMovieItem> items;
        private Params params;
        private List<BreadCrumb> breadCrumb;
        private String titlePage;

        @JsonProperty("type_list")
        private String typeList;

        @JsonProperty("APP_DOMAIN_CDN_IMAGE")
        private String appDomainCdnImage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BreadCrumb {
        private String name;
        private String slug;
        private Boolean isCurrent;
        private Integer position;
    }

    /**
     * Tạo empty response khi không tìm thấy kết quả
     */
    public static ListData emptyListData() {
        Params.Pagination pagination = new Params.Pagination();
        pagination.setTotalItems(0);
        pagination.setTotalItemsPerPage(24);
        pagination.setCurrentPage(1);
        pagination.setPageRanges(0);

        Params params = new Params();
        params.setPagination(pagination);

        return ListData.builder()
                .seoOnPage(null)
                .items(new ArrayList<>())
                .params(params)
                .breadCrumb(new ArrayList<>())
                .titlePage("")
                .typeList("")
                .appDomainCdnImage("https://img.ophim.live")
                .build();
    }
}
