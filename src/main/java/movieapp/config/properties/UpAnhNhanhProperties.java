package movieapp.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "upanhnhanh")
public class UpAnhNhanhProperties {
    private String apiKey;
    private String baseUrl = "https://www.upanhnhanh.com/api/v1";

}
