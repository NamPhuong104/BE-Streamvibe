package movieapp.config;

import movieapp.config.properties.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        AppProperties.class,
        OPhimProperties.class,
        JwtProperties.class,
        CloudinaryProperties.class,
        GoogleProperties.class,
        CacheProperties.class,
        MailProperties.class
})
public class PropertiesConfig {
}
