package movieapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.config.properties.OPhimProperties;
import movieapp.dto.OphimResponse.OphimListResponse;
import movieapp.dto.OphimResponse.OphimMovieItem;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovieProxyService {
    // ==================== FILTER CONSTANTS ====================
    private static final Set<String> BLOCKED_CATEGORIES = Set.of("phim-18", "phim 18+", "18+");
    private static final Set<String> BLOCKED_EPISODE_STATUS = Set.of("trailer");
    private final RestTemplate restTemplate;
    private final OPhimProperties oPhimProperties;
    private final BlockedKeywordService blockedKeywordService;

    public OphimListResponse.ListData search(String keyword, int page, int limit) {
        if (blockedKeywordService.isKeywordBlocked(keyword)) return OphimListResponse.emptyListData();

        String url = UriComponentsBuilder
                .fromHttpUrl(oPhimProperties.getBaseUrl() + "/tim-kiem")
                .queryParam("keyword", keyword)
                .queryParam("page", page)
                .queryParam("limit", limit)
                .build()
                .toUriString();

        try {
            OphimListResponse response = restTemplate.getForObject(url, OphimListResponse.class);
            if (response == null || response.getData() == null) return OphimListResponse.emptyListData();

            filterInPlace(response.getData());

            return response.getData();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tìm kiếm phim", e);
        }
    }

    // ==================== LIST BY SLUG (danh-sach) ====================
    public OphimListResponse.ListData getListBySlug(String slug, Map<String, String> params) {
        String url = buildUrl("/danh-sach/" + slug, params);


        try {
            OphimListResponse response = restTemplate.getForObject(url, OphimListResponse.class);

            if (response == null || response.getData() == null) {
                return OphimListResponse.emptyListData();
            }

            filterInPlace(response.getData());
            return response.getData();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách phim", e);
        }
    }

    // ==================== BY CATEGORY (the-loai) ====================

    public OphimListResponse.ListData getByCategory(String slug, Map<String, String> params) {
        String url = buildUrl("/the-loai/" + slug, params);

        try {
            OphimListResponse response = restTemplate.getForObject(url, OphimListResponse.class);

            if (response == null || response.getData() == null) {
                return OphimListResponse.emptyListData();
            }

            filterInPlace(response.getData());
            return response.getData();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy phim theo thể loại", e);
        }
    }

    // ==================== BY COUNTRY (quoc-gia) ====================

    public OphimListResponse.ListData getByCountry(String slug, Map<String, String> params) {
        String url = buildUrl("/quoc-gia/" + slug, params);

        try {
            OphimListResponse response = restTemplate.getForObject(url, OphimListResponse.class);

            if (response == null || response.getData() == null) {
                return OphimListResponse.emptyListData();
            }

            filterInPlace(response.getData());
            return response.getData();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy phim theo quốc gia", e);
        }
    }


    // ==================== HELPER METHODS ====================

    private String buildUrl(String path, Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(oPhimProperties.getBaseUrl() + path);

        if (params != null) {
            params.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    builder.queryParam(key, value);
                }
            });
        }

        return builder.build().toUriString();
    }

    /**
     * Filter in place - remove 18+ and trailer content
     */
    private void filterInPlace(OphimListResponse.ListData data) {
        if (data == null || data.getItems() == null) {
            return;
        }

        List<OphimMovieItem> items = data.getItems();
        int originalSize = items.size();

        List<OphimMovieItem> filtered = items.stream()
                .filter(item -> !isTrailer(item))
                .filter(item -> !isAdultContent(item))
                .collect(Collectors.toList());

        data.setItems(filtered);

        // Update pagination
        if (data.getParams() != null && data.getParams().getPagination() != null) {
            int removedCount = originalSize - filtered.size();
            if (removedCount > 0) {
                var pagination = data.getParams().getPagination();
                int newTotal = Math.max(0, pagination.getTotalItems() - removedCount);
                pagination.setTotalItems(newTotal);

                // Recalculate pageRanges
                int itemsPerPage = pagination.getTotalItemsPerPage();
                if (itemsPerPage > 0) {
                    pagination.setPageRanges((int) Math.ceil((double) newTotal / itemsPerPage));
                }
            }
        }
    }

    private boolean isTrailer(OphimMovieItem item) {
        if (item.getEpisodeCurrent() == null) return false;
        return BLOCKED_EPISODE_STATUS.contains(item.getEpisodeCurrent().toLowerCase().trim());
    }

    private boolean isAdultContent(OphimMovieItem item) {
        if (item.getCategory() == null || item.getCategory().isEmpty()) return false;
        return item.getCategory().stream()
                .anyMatch(cat -> {
                    String slug = cat.getSlug() != null ? cat.getSlug().toLowerCase() : "";
                    String name = cat.getName() != null ? cat.getName().toLowerCase() : "";
                    return BLOCKED_CATEGORIES.contains(slug) || BLOCKED_CATEGORIES.contains(name);
                });
    }

}
