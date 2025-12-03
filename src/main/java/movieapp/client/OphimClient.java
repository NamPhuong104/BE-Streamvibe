package movieapp.client;

import lombok.extern.slf4j.Slf4j;
import movieapp.dto.OphimResponse.OphimHomepageResponse;
import movieapp.dto.OphimResponse.OphimListResponse;
import movieapp.dto.OphimResponse.OphimMovieDetailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
public class OphimClient {
    @Value("${ophim.baseurl}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public OphimClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public OphimHomepageResponse getHomepage() {
        String url = baseUrl + "/home";
        log.info("🔄 Calling Ophim API: {}", url);

        try {
            OphimHomepageResponse response = restTemplate.getForObject(url, OphimHomepageResponse.class);
            log.info("✅ Received {} items from Ophim",
                    response != null && response.getData() != null
                            ? response.getData().getItems().size()
                            : 0);
            return response;
        } catch (Exception e) {
            log.error("❌ Error calling Ophim API: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch homepage from Ophim", e);
        }
    }

    public OphimMovieDetailResponse getMovieDetail(String slug) {
        String url = baseUrl + "/phim/" + slug;
        log.debug("🔄 Calling Ophim API for movie: {}", slug);

        try {
            OphimMovieDetailResponse response = restTemplate.getForObject(url, OphimMovieDetailResponse.class);
            log.debug("✅ Received detail for: {}", slug);
            return response;
        } catch (Exception e) {
            log.error("❌ Error fetching movie detail for {}: {}", slug, e.getMessage());
            throw new RuntimeException("Failed to fetch movie: " + slug, e);
        }
    }

    public OphimListResponse getListBySlug(String slug, Map<String, String> params) {
        StringBuilder url = new StringBuilder(baseUrl + "/danh-sach/" + slug);
        if (params != null && !params.isEmpty()) {
            url.append("?");
            params.forEach((key, value) -> {
                url.append(key).append("=").append(value).append("&");
            });
            url.deleteCharAt(url.length() - 1);
        }
        log.debug("🔄 Calling Ophim API: {}", url);

        try {
            OphimListResponse response = restTemplate.getForObject(url.toString(), OphimListResponse.class);
            log.debug("✅ Received {} items",
                    response != null && response.getData() != null
                            ? response.getData().getItems().size()
                            : 0);
            return response;
        } catch (Exception e) {
            log.error("❌ Error calling Ophim API {}: {}", url, e.getMessage());
            throw new RuntimeException("Failed to fetch list: " + slug, e);
        }
    }
}
