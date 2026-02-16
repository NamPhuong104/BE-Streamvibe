package movieapp.dto.OphimResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OphimImageResponse {
    private boolean success;
    private String message;

    @JsonProperty("status_code")
    private int statusCode;

    private ImageData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageData {
        private String slug;

        @JsonProperty("image_sizes")
        private ImageSizes imageSizes;

        private List<ImageItem> images;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageSizes {
        private SizeMap backdrop;
        private SizeMap poster;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SizeMap {
        private String original;
        private String w1280;
        private String w780;
        private String w300;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageItem {
        private int width;
        private int height;

        @JsonProperty("aspect_ratio")
        private double aspectRatio;

        private String type; // "backdrop" or "poster"

        @JsonProperty("file_path")
        private String filePath;

        @JsonProperty("iso_639_1")
        private String language;
    }
}
