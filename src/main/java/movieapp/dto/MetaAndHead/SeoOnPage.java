package movieapp.dto.MetaAndHead;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SeoOnPage {
    @JsonProperty("og_type")
    private String ogType;

    @JsonProperty("titleHead")
    private String titleHead;

    @JsonProperty("descriptionHead")
    private String descriptionHead;

    @JsonProperty("og_image")
    private List<String> ogImage;
}
