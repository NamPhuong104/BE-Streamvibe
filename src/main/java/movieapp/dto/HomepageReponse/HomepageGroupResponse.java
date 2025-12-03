package movieapp.dto.HomepageReponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import movieapp.dto.CustomFieldsResponse.MovieItemDTO;
import movieapp.dto.MetaAndHead.SeoOnPage;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomepageGroupResponse {
    private SeoOnPage seoOnPage;
    private String message;
    private Long cachedAt;
    private String group;
    private boolean hasMore;
    private String nextGroup;


    private List<MovieItemDTO> section1;
    private HomepageResponse.Section2Data section2;
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
}
