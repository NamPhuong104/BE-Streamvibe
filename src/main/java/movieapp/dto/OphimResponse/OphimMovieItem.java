package movieapp.dto.OphimResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class OphimMovieItem {
    @JsonProperty("_id")
    private String id;

    private String name;
    private String slug;

    @JsonProperty("origin_name")
    private String originName;

    private String type;
    private String status;

    @JsonProperty("thumb_url")
    private String thumbUrl;

    @JsonProperty("poster_url")
    private String posterUrl;

    @JsonProperty("is_copyright")
    private Boolean isCopyright;

    @JsonProperty("trailer_url")
    private String trailerUrl;

    private String time;

    @JsonProperty("episode_current")
    private String episodeCurrent;

    @JsonProperty("episode_total")
    private String episodeTotal;

    private String quality;
    private String lang;
    private Integer year;

    private List<String> actor;
    private List<String> director;

    private Tmdb tmdb;
    private Imdb imdb;
    private Modified modified;

    private List<Category> category;
    private List<Country> country;

    @JsonProperty("alternative_names")
    private List<String> alternativeNames;

    @Data
    public static class Tmdb {
        private String type;
        private String id;
        private Integer season;

        @JsonProperty("vote_average")
        private Double voteAverage;

        @JsonProperty("vote_count")
        private Integer voteCount;
    }

    @Data
    public static class Imdb {
        private String id;

        @JsonProperty("vote_average")
        private Double voteAverage;

        @JsonProperty("vote_count")
        private Integer voteCount;
    }

    @Data
    public static class Modified {
        private String time;
    }

    @Data
    public static class Category {
        private String id;
        private String name;
        private String slug;
    }

    @Data
    public static class Country {
        private String id;
        private String name;
        private String slug;
    }

}