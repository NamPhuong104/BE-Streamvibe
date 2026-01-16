package movieapp.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String frontendUrl = "http://localhost:3000";
    private ImageConfig image = new ImageConfig();

    @Data
    public static class ImageConfig {
        private boolean enableCloudinary = true;
    }

    /**
     * Kiểm tra có phải môi trường production không
     */
    public boolean isProduction() {
        return frontendUrl != null && !frontendUrl.contains("localhost");
    }

    /**
     * Lấy allowed origins cho CORS
     */
    public String[] getCorsOrigins() {
        return new String[]{frontendUrl};
    }
}
