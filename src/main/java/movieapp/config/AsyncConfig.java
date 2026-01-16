package movieapp.config;

import lombok.RequiredArgsConstructor;
import movieapp.config.properties.CacheProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig {
    private final CacheProperties cacheProperties;

    @Bean(name = "taskExecutor", destroyMethod = "shutdown")
    public ExecutorService taskExecutor() {
        return Executors.newFixedThreadPool(cacheProperties.getDetailFetchThreads());
    }
}