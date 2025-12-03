package movieapp.dto.OphimResponse;

import lombok.Data;
import movieapp.dto.MetaAndHead.SeoOnPage;

@Data
public class OphimMovieDetailResponse {
    private String status;
    private String message;
    private MovieDetailData data;

    @Data
    public static class MovieDetailData {
        private SeoOnPage seoOnPage;
        private OphimMovieDetail item;
    }
}
