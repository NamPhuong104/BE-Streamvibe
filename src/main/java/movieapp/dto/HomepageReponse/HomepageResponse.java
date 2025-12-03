package movieapp.dto.HomepageReponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import movieapp.dto.CustomFieldsResponse.MovieItemDTO;
import movieapp.dto.OphimResponse.OphimHomepageResponse;
import movieapp.dto.MetaAndHead.SeoOnPage;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomepageResponse {
    private SeoOnPage seoOnPage;
    private String message;
    private Long cachedAt;

    private OphimHomepageResponse rawData;
    private List<MovieItemDTO> section1;
    private Section2Data section2;
    private List<MovieItemDTO> section3;
    private List<MovieItemDTO> section4;
    private List<MovieItemDTO> section5;
    private List<MovieItemDTO> section6;
    private List<MovieItemDTO> section7;
    private List<MovieItemDTO> section8;
    private List<MovieItemDTO> section9;
    private List<MovieItemDTO> section10;
    private List<MovieItemDTO> section11;
    private List<MovieItemDTO> section12;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Section2Data {
        private List<MovieItemDTO> ListKorea;
        private List<MovieItemDTO> ListChina;
        private List<MovieItemDTO> ListUSAndUK;
    }
}
