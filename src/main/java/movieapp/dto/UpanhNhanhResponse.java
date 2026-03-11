package movieapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class UpanhNhanhResponse {
    private boolean success;
    private List<String> urls;
    private List<ImageData> data;
    private List<String> errors;

    @Data
    public static class ImageData {
        @JsonProperty("proxy_url")
        private String proxyUrl;

        @JsonProperty("thumbnail_url")
        private String thumbnailUrl;
    }
}
