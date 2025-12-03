package movieapp.dto.OphimResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class OphimMovieDetail extends OphimMovieItem {
    private String content;
    private List<Episode> episodes;

    @Data
    public static class Episode {
        @JsonProperty("server_name")
        private String serverName;

        @JsonProperty("is_ai")
        private Boolean isAi;

        @JsonProperty("server_data")
        private List<EpisodeData> serverData;
    }

    @Data
    public static class EpisodeData {
        private String name;
        private String slug;
        private String filename;

        @JsonProperty("link_embed")
        private String linkEmbed;

        @JsonProperty("link_m3u8")
        private String linkM3u8;
    }
}
