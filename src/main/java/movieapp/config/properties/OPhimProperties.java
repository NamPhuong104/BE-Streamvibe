package movieapp.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ophim")
public class OPhimProperties {
    private String baseUrl = "https://ophim1.com/v1/api";
    private String fullUrlImage = "https://img.ophim.live/uploads/movies";

    public String getFullImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return null;

        if (imagePath.startsWith("http")) return imagePath;

        return fullUrlImage + "/" + imagePath;
    }
}
