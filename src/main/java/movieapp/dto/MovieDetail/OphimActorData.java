package movieapp.dto.MovieDetail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OphimActorData {
    private Long tmdbId;
    private String tmdbType;
    private String ophimId;
    private String slug;
    private String imdbId;
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
        private Long tmdbPeopleId;
        private Boolean adult;
        private Long gender;
        private String genderName;
        private String name;
        private String originalName;
        private String character;
        private String knownForDepartment;
        private String profilePath;
        private List<String> alsoKnowAs;
    }
}
