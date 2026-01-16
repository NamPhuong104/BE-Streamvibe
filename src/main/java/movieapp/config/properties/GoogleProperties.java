package movieapp.config.properties;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "google")
public class GoogleProperties {
    private String clientId;

    public boolean isConfigured() {
        return clientId != null && !clientId.isEmpty();
    }
}
