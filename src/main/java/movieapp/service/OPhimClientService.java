package movieapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.config.properties.OPhimProperties;
import movieapp.dto.MovieDetail.OphimActorApiResponse;
import movieapp.dto.MovieDetail.OphimActorData;
import movieapp.dto.OphimResponse.OphimHomepageResponse;
import movieapp.dto.OphimResponse.OphimListResponse;
import movieapp.dto.OphimResponse.OphimMovieDetailResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class OPhimClientService {
    private final RestTemplate restTemplate;
    private final OPhimProperties oPhimProperties;


    public OphimHomepageResponse getHomepage() {
        String url = oPhimProperties.getBaseUrl() + "/home";
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
        String url = oPhimProperties.getBaseUrl() + "/phim/" + slug;
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
        StringBuilder url = new StringBuilder(oPhimProperties.getBaseUrl() + "/danh-sach/" + slug);
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

    public OphimActorData getMovieActors(String slug) {
        String url = oPhimProperties.getBaseUrl() + "/phim/" + slug + "/peoples";
        try {
            OphimActorApiResponse response = restTemplate.getForObject(url, OphimActorApiResponse.class);
            return response != null && response.isSuccess() ? response.getData() : null;
        } catch (Exception e) {
            log.error("⚠\uFE0F Failed to fetch actors for {}: {}", url, e.getMessage());
            return null;
        }
    }
}
