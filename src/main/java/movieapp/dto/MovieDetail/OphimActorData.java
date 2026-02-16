package movieapp.dto.MovieDetail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OphimActorData {
    @JsonProperty("tmdb_id")
    private Long tmdbId;

    @JsonProperty("tmdb_type")
    private String tmdbType;

    @JsonProperty("ophim_id")
    private String ophimId;

    private String slug;

    @JsonProperty("imdb_id")
    private String imdbId;

    @JsonProperty("profile_sizes")
    private ProfileSizes profileSizes;

    private List<People> peoples;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProfileSizes {
        private String h632;
        private String original;
        private String w185;
        private String w45;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class People {
        @JsonProperty("tmdb_people_id")
        private Long tmdbPeopleId;

        private Boolean adult;
        private Long gender;

        @JsonProperty("gender_name")
        private String genderName;

        private String name;

        @JsonProperty("original_name")
        private String originalName;

        private String character;

        @JsonProperty("known_for_department")
        private String knownForDepartment;

        @JsonProperty("profile_path")
        private String profilePath;

        @JsonProperty("also_known_as")
        private List<String> alsoKnowAs;
    }
}
