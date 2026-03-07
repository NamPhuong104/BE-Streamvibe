package movieapp.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "spring.mail")
public class MailProperties {
    private final String username;
    private final String password;

    /**
     * Kiểm tra mail đã cấu hình chưa
     */
    public boolean isConfigured() {
        return username != null && !username.isEmpty()
                && password != null && !password.isEmpty();
    }

    /**
     * Lấy domain từ email
     */
    public String getEmailDomain() {
        if (username == null || !username.contains("@")) {
            return null;
        }
        return username.substring(username.indexOf("@") + 1);
    }
}
