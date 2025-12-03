package movieapp.dto.OphimResponse;

import lombok.Data;
import movieapp.dto.MetaAndHead.Params;
import movieapp.dto.MetaAndHead.SeoOnPage;

import java.util.List;

@Data
public class OphimListResponse {
    private String status;
    private String message;
    private ListData data;

    @Data
    public static class ListData {
        private SeoOnPage seoOnPage;
        private List<OphimMovieItem> items;
        private Params params;
    }
}
