package movieapp.service;

import lombok.extern.slf4j.Slf4j;
import movieapp.dto.CustomFieldsResponse.MovieItemDTO;
import movieapp.dto.HomepageReponse.HomepageResponse;
import movieapp.dto.OphimResponse.OphimHomepageResponse;
import movieapp.dto.OphimResponse.OphimListResponse;
import movieapp.dto.OphimResponse.OphimMovieDetailResponse;
import movieapp.dto.OphimResponse.OphimMovieItem;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CachedSectionService {
    private static final Set<String> BLOCKED_CATEGORIES = Set.of(
            "phim-18",
            "phim 18+",
            "18+"
    );
    private static final Set<String> BLOCKED_EPISODE_STATUS = Set.of(
            "trailer"
    );
    // Số phim cần lấy mặc định
    private static final int DEFAULT_LIMIT = 14;

    // ==================== FILTER CONSTANTS ====================
    // Multiplier để fetch nhiều hơn (đề phòng bị filter bớt)
    private static final int FETCH_MULTIPLIER = 3;
    // Số page tối đa để fetch
    private static final int MAX_PAGES = 5;
    private final OPhimClientService ophimClient;
    private final ImageOptimizationService imageService;
    private final ExecutorService executorService;

    public CachedSectionService(OPhimClientService ophimClient,
                                ImageOptimizationService imageService,
                                @Qualifier("taskExecutor") ExecutorService executorService) {
        this.ophimClient = ophimClient;
        this.imageService = imageService;
        this.executorService = executorService;
    }

    // ==================== FILTER PREDICATES ====================

    /**
     * Kiểm tra phim có phải Trailer không
     */
    private boolean isTrailer(OphimMovieItem item) {
        if (item.getEpisodeCurrent() == null) return false;
        return BLOCKED_EPISODE_STATUS.contains(item.getEpisodeCurrent().toLowerCase().trim());
    }

    /**
     * Kiểm tra phim có phải 18+ không
     */
    private boolean isAdultContent(OphimMovieItem item) {
        if (item.getCategory() == null || item.getCategory().isEmpty()) return false;

        return item.getCategory().stream()
                .anyMatch(cat -> {
                    String slug = cat.getSlug() != null ? cat.getSlug().toLowerCase() : "";
                    String name = cat.getName() != null ? cat.getName().toLowerCase() : "";
                    return BLOCKED_CATEGORIES.contains(slug) || BLOCKED_CATEGORIES.contains(name);
                });
    }

    /**
     * Predicate: Phim hợp lệ (không phải Trailer và không phải 18+)
     */
    private Predicate<OphimMovieItem> isValidMovie() {
        return item -> !isTrailer(item) && !isAdultContent(item);
    }

    /**
     * Filter danh sách phim
     */
    private List<OphimMovieItem> filterValidMovies(List<OphimMovieItem> items) {
        return items.stream()
                .filter(isValidMovie())
                .collect(Collectors.toList());
    }

    // ==================== FETCH RAW OPHIM ====================

    @Cacheable(value = "homepage", key = "'raw'")
    public OphimHomepageResponse fetchHomepageRaw() {
        log.info("📥 Fetching raw homepage data from Ophim...");
        return ophimClient.getHomepage();
    }

    // ==================== SECTION 1: Hero Banner (7 items với detail) ====================

    @Cacheable(value = "homepage", key = "'section1'")
    public List<MovieItemDTO> fetchSection1() {
        log.info("📥 Fetching Section 1 (7 items with POSTER + CONTENT)...");
        OphimHomepageResponse rawData = fetchHomepageRaw();
        List<OphimMovieItem> rawItems = rawData.getData().getItems();

        // Filter và lấy 7 items đầu tiên
        List<OphimMovieItem> validItems = filterValidMovies(rawItems)
                .stream()
                .limit(7)
                .collect(Collectors.toList());

        log.info("📊 Section 1: {} valid items after filtering", validItems.size());
        return fetchItemsWithDetailParallel(validItems);
    }

    // ==================== SECTION 2: Tabs (Korea, China, US/UK) ====================

    @Cacheable(value = "homepage", key = "'section2'")
    public HomepageResponse.Section2Data fetchSection2() {
        log.info("📥 Fetching Section 2...");
        List<MovieItemDTO> listKorea = listKoreaRaw();
        List<MovieItemDTO> listChina = listChinaRaw();
        List<MovieItemDTO> listUSUK = listUSUKRaw();

        return HomepageResponse.Section2Data.builder()
                .ListKorea(listKorea)
                .ListChina(listChina)
                .ListUSAndUK(listUSUK)
                .build();
    }

    public List<MovieItemDTO> listKoreaRaw() {
        log.info("📥 Fetching Section 2 List Korea...");
        Map<String, String> params = new HashMap<>();
        params.put("country", "han-quoc");
        params.put("sort_field", "year");
        params.put("sort_type", "desc");

        return fetchListSectionWithFilter("phim-moi", params, DEFAULT_LIMIT);
    }

    public List<MovieItemDTO> listChinaRaw() {
        log.info("📥 Fetching Section 2 List China...");
        Map<String, String> params = new HashMap<>();
        params.put("country", "trung-quoc");
        params.put("sort_field", "year");
        params.put("sort_type", "desc");

        return fetchListSectionWithFilter("phim-moi", params, DEFAULT_LIMIT);
    }

    public List<MovieItemDTO> listUSUKRaw() {
        log.info("📥 Fetching Section 2 List US/UK...");
        Map<String, String> params = new HashMap<>();
        params.put("country", "au-my");
        params.put("sort_field", "year");
        params.put("sort_type", "desc");

        return fetchListSectionWithFilter("phim-moi", params, DEFAULT_LIMIT);
    }

    // ==================== SECTION 3-12 ====================

    @Cacheable(value = "homepage", key = "'section3'")
    public List<MovieItemDTO> fetchSection3() {
        log.info("📥 Fetching Section 3 List Series...");
        Map<String, String> params = new HashMap<>();
        params.put("sort_field", "year");
        params.put("sort_type", "desc");

        return fetchListSectionWithFilter("phim-bo", params, DEFAULT_LIMIT);
    }

    @Cacheable(value = "homepage", key = "'section4'")
    public List<MovieItemDTO> fetchSection4() {
        log.info("📥 Fetching Section 4 List Action...");
        Map<String, String> params = new HashMap<>();
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("category", "hanh-dong");

        return fetchListSectionWithFilter("phim-chieu-rap", params, DEFAULT_LIMIT);
    }

    @Cacheable(value = "homepage", key = "'section5'")
    public List<MovieItemDTO> fetchSection5() {
        log.info("📥 Fetching Section 5 List Single...");
        Map<String, String> params = new HashMap<>();
        params.put("sort_field", "year");
        params.put("sort_type", "desc");

        return fetchListSectionWithFilter("phim-le", params, DEFAULT_LIMIT);
    }

    @Cacheable(value = "homepage", key = "'section6'")
    public List<MovieItemDTO> fetchSection6() {
        log.info("📥 Fetching Section 6 (14 items from homepage raw)...");
        OphimHomepageResponse rawData = fetchHomepageRaw();
        List<OphimMovieItem> rawItems = rawData.getData().getItems();

        // Skip 7 items đầu (đã dùng cho section 1), filter, lấy 14
        List<OphimMovieItem> validItems = rawItems.stream()
                .skip(7)
                .filter(isValidMovie())
                .limit(DEFAULT_LIMIT)
                .collect(Collectors.toList());

        log.info("📊 Section 6: {} valid items after filtering", validItems.size());
        return validItems.stream()
                .map(this::processItemWithoutDetail)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "homepage", key = "'section7'")
    public List<MovieItemDTO> fetchSection7() {
        log.info("📥 Fetching Section 7 List Horror...");
        Map<String, String> params = new HashMap<>();
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("category", "kinh-di");
        params.put("country", "thai-lan");

        return fetchListSectionWithFilter("phim-moi", params, DEFAULT_LIMIT);
    }

    @Cacheable(value = "homepage", key = "'section8'")
    public List<MovieItemDTO> fetchSection8() {
        log.info("📥 Fetching Section 8 List Korea Love...");
        Map<String, String> params = new HashMap<>();
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("category", "tinh-cam");
        params.put("country", "han-quoc");

        return fetchListSectionWithFilter("phim-moi", params, DEFAULT_LIMIT);
    }

    @Cacheable(value = "homepage", key = "'section9'")
    public List<MovieItemDTO> fetchSection9() {
        log.info("📥 Fetching Section 9 (Cartoon with content)...");
        Map<String, String> params = new HashMap<>();
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("country", "nhat-ban,han-quoc");

        // Fetch với filter, sau đó lấy detail
        List<OphimMovieItem> validItems = fetchRawItemsWithFilter("hoat-hinh", params, DEFAULT_LIMIT);
        return fetchItemsWithDetailParallel(validItems);
    }

    @Cacheable(value = "homepage", key = "'section10'")
    public List<MovieItemDTO> fetchSection10() {
        log.info("📥 Fetching Section 10 List Crime...");
        Map<String, String> params = new HashMap<>();
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("category", "hinh-su");

        return fetchListSectionWithFilter("phim-moi", params, DEFAULT_LIMIT);
    }

    @Cacheable(value = "homepage", key = "'section11'")
    public List<MovieItemDTO> fetchSection11() {
        log.info("📥 Fetching Section 11 List Secret...");
        Map<String, String> params = new HashMap<>();
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("category", "bi-an");

        return fetchListSectionWithFilter("phim-moi", params, DEFAULT_LIMIT);
    }

    @Cacheable(value = "homepage", key = "'section12'")
    public List<MovieItemDTO> fetchSection12() {
        log.info("📥 Fetching Section 12 List Adventure...");
        Map<String, String> params = new HashMap<>();
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("category", "phieu-luu");

        return fetchListSectionWithFilter("phim-moi", params, DEFAULT_LIMIT);
    }

    // ==================== HELPER METHODS ====================

    /**
     * ✅ CORE METHOD: Fetch list với filter, đảm bảo đủ số lượng
     * <p>
     * Logic:
     * 1. Fetch page 1 với limit lớn (gấp 3 lần cần thiết)
     * 2. Filter bỏ Trailer và 18+
     * 3. Nếu đủ số lượng → return
     * 4. Nếu không đủ → fetch thêm page tiếp theo
     * 5. Repeat cho đến khi đủ hoặc hết data
     */
    private List<MovieItemDTO> fetchListSectionWithFilter(String slug, Map<String, String> baseParams, int requiredCount) {
        List<OphimMovieItem> validItems = fetchRawItemsWithFilter(slug, baseParams, requiredCount);

        return validItems.stream()
                .map(this::processItemWithoutDetail)
                .collect(Collectors.toList());
    }

    /**
     * Fetch raw items với filter, đảm bảo đủ số lượng
     */
    private List<OphimMovieItem> fetchRawItemsWithFilter(String slug, Map<String, String> baseParams, int requiredCount) {
        List<OphimMovieItem> collectedItems = new ArrayList<>();
        int currentPage = 1;
        int fetchLimit = requiredCount * FETCH_MULTIPLIER; // Fetch gấp 3 lần

        while (collectedItems.size() < requiredCount && currentPage <= MAX_PAGES) {
            try {
                Map<String, String> params = new HashMap<>(baseParams);
                params.put("page", String.valueOf(currentPage));
                params.put("limit", String.valueOf(fetchLimit));

                log.debug("📄 Fetching page {} with limit {} for {}", currentPage, fetchLimit, slug);

                OphimListResponse response = ophimClient.getListBySlug(slug, params);

                if (response == null || response.getData() == null ||
                        response.getData().getItems() == null || response.getData().getItems().isEmpty()) {
                    log.warn("⚠️ No more data available at page {}", currentPage);
                    break;
                }

                List<OphimMovieItem> items = response.getData().getItems();
                List<OphimMovieItem> validItems = filterValidMovies(items);

                log.debug("📊 Page {}: {} total, {} valid after filter",
                        currentPage, items.size(), validItems.size());

                collectedItems.addAll(validItems);

                // Nếu page này trả về ít hơn limit → đã hết data
                if (items.size() < fetchLimit) {
                    log.debug("📊 Reached end of data at page {}", currentPage);
                    break;
                }

                currentPage++;

            } catch (Exception e) {
                log.error("❌ Error fetching page {} for {}: {}", currentPage, slug, e.getMessage());
                break;
            }
        }

        // Limit lại đúng số lượng cần
        List<OphimMovieItem> result = collectedItems.stream()
                .limit(requiredCount)
                .collect(Collectors.toList());

        log.info("📊 {} - Collected {} valid items (required: {})", slug, result.size(), requiredCount);

        if (result.size() < requiredCount) {
            log.warn("⚠️ {} - Only got {} items, less than required {}", slug, result.size(), requiredCount);
        }

        return result;
    }

    /**
     * Fetch items với detail (parallel)
     */
    private List<MovieItemDTO> fetchItemsWithDetailParallel(List<OphimMovieItem> items) {
        log.info("🚀 Fetching detail for {} items in PARALLEL...", items.size());

        List<CompletableFuture<MovieItemDTO>> futures = items.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> processItemWithDetail(item), executorService))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    /**
     * Process item với detail (có content)
     */
    private MovieItemDTO processItemWithDetail(OphimMovieItem item) {
        MovieItemDTO dto = new MovieItemDTO();
        BeanUtils.copyProperties(item, dto);

        try {
            log.debug("📄 Fetching content for: {}", item.getSlug());
            OphimMovieDetailResponse detailResponse = ophimClient.getMovieDetail(item.getSlug());
            var detailItem = detailResponse.getData().getItem();

            // Set content
            dto.setContent(detailItem.getContent());

            // Update poster_url và thumb_url từ detail nếu list trả về null
            String detailPosterUrl = detailItem.getPosterUrl();
            String detailThumbUrl = detailItem.getThumbUrl();

            if ((dto.getPosterUrl() == null || dto.getPosterUrl().isEmpty()) && detailPosterUrl != null) {
                dto.setPosterUrl(detailPosterUrl);
            }
            if ((dto.getThumbUrl() == null || dto.getThumbUrl().isEmpty()) && detailThumbUrl != null) {
                dto.setThumbUrl(detailThumbUrl);
            }

            // Optimize images từ detail API
            dto.setOptimizedThumb(imageService.optimizeThumb(detailThumbUrl, detailItem.getSlug()));
            dto.setOptimizedPoster(imageService.optimizedPoster(detailPosterUrl, detailItem.getSlug()));

        } catch (Exception e) {
            log.warn("⚠️ Failed to fetch content for {}: {}", item.getSlug(), e.getMessage());
            dto.setContent(null);
            // Fallback: optimize từ list API data
            dto.setOptimizedThumb(imageService.optimizeThumb(item.getThumbUrl(), item.getSlug()));
            dto.setOptimizedPoster(imageService.optimizedPoster(item.getPosterUrl(), item.getSlug()));
        }
        return dto;
    }

    /**
     * Process item không có detail (chỉ optimize ảnh)
     */
    private MovieItemDTO processItemWithoutDetail(OphimMovieItem item) {
        MovieItemDTO dto = new MovieItemDTO();
        BeanUtils.copyProperties(item, dto);

        dto.setOptimizedThumb(imageService.optimizeThumb(item.getThumbUrl(), item.getSlug()));
        dto.setOptimizedPoster(imageService.optimizedPoster(item.getPosterUrl(), item.getSlug()));
        dto.setContent(null);

        return dto;
    }
}