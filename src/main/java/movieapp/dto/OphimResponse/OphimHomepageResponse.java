package movieapp.dto.OphimResponse;

import lombok.Data;
import movieapp.dto.MetaAndHead.Params;
import movieapp.dto.MetaAndHead.SeoOnPage;

import java.util.List;

@Data
public class OphimHomepageResponse {
    private String status;
    private String message;
    private HomepageData data;

    @Data
    public static class HomepageData {
        private SeoOnPage seoOnPage;
        private List<OphimMovieItem> items;
        private Params params;
    }
}
