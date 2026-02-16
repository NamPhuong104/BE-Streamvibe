package movieapp.service;

import lombok.extern.slf4j.Slf4j;
import movieapp.dto.CustomFieldsResponse.MovieItemDTO;
import movieapp.dto.HomepageReponse.HomepageResponse;
import movieapp.dto.OphimResponse.*;
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

    // ==================== FILTER CONSTANTS ====================
    private static final Set<String> BLOCKED_CATEGORIES = Set.of("phim-18", "phim 18+", "18+");
    private static final Set<String> BLOCKED_EPISODE_STATUS = Set.of("trailer");
    private static final Set<String> BLOCKED_TYPE = Set.of("hoathinh");

    // ==================== SECTION CONFIGS ====================
    private static final int SECTION1_REQUIRED_COUNT = 7;
    private static final int SECTION6_REQUIRED_COUNT = 14;
    private static final int DEFAULT_LIMIT = 14;
    private static final int FETCH_MULTIPLIER = 3;
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

    private boolean isAnime(OphimMovieItem item) {
        if (item.getType() == null || item.getType().isEmpty()) return false;
        return BLOCKED_TYPE.contains(item.getType().trim().toLowerCase());
    }

    private boolean isPosterNull(OphimMovieItem item) {
        if (item.getPosterUrl() == null || item.getPosterUrl().isEmpty()) return true;

        return false;
    }

    private boolean isThumbNull(OphimMovieItem item) {
        if (item.getThumbUrl() == null || item.getThumbUrl().isEmpty()) return true;

        return false;
    }

    // ==================== PREDICATES ====================

    /**
     * Predicate cho sections thường: !18+ && !trailer && !hoathinh
     */
    private Predicate<OphimMovieItem> isValidMovie() {
        return item -> !isTrailer(item) && !isAdultContent(item) && !isAnime(item);
    }

    /**
     * Predicate cho section 9 (hoạt hình): !18+ && !trailer
     */
    private Predicate<OphimMovieItem> isValidAnimation() {
        return item -> !isTrailer(item) && !isAdultContent(item);
    }

    // ==================== PLAYABLE CHECK ====================

    private boolean hasPlayableEpisodes(OphimMovieDetail detail) {
        if (detail == null) return false;
        List<OphimMovieDetail.Episode> episodes = detail.getEpisodes();
        if (episodes == null || episodes.isEmpty()) return false;

        return episodes.stream()
                .filter(ep -> ep.getServerData() != null && !ep.getServerData().isEmpty())
                .flatMap(ep -> ep.getServerData().stream())
                .anyMatch(this::isEpisodeDataPlayable);
    }

    private boolean isEpisodeDataPlayable(OphimMovieDetail.EpisodeData episodeData) {
        if (episodeData == null) return false;
        String m3u8 = episodeData.getLinkM3u8();
        String embed = episodeData.getLinkEmbed();
        return (m3u8 != null && !m3u8.trim().isEmpty()) || (embed != null && !embed.trim().isEmpty());
    }

    // ==================== FETCH RAW OPHIM ====================

    @Cacheable(value = "homepage", key = "'raw'")
    public OphimHomepageResponse fetchHomepageRaw() {
        log.info("📥 Fetching raw homepage data from Ophim...");
        return ophimClient.getHomepage();
    }

    // ==================== SECTION 1: Hero Banner ====================

    @Cacheable(value = "homepage", key = "'section1'")
    public List<MovieItemDTO> fetchSection1() {
        log.info("📥 Fetching Section 1 (7 PLAYABLE items)...");
        long startTime = System.currentTimeMillis();

        OphimHomepageResponse rawData = fetchHomepageRaw();
        List<OphimMovieItem> rawItems = rawData.getData().getItems();

        log.info("📊 Section 1: Raw {} items", rawItems.size());

        // Pre-filter: !18+ && !trailer && !hoathinh
        List<OphimMovieItem> preFilterItems = rawItems.stream()
                .filter(isValidMovie())
                .collect(Collectors.toList());

        log.info("📊 Section 1: {} items after pre-filter", preFilterItems.size());

        // Fetch detail + check playable
        List<MovieItemDTO> playableMovies = fetchPlayableItemsParallel(preFilterItems, SECTION1_REQUIRED_COUNT);

        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ Section 1: {} PLAYABLE movies in {}ms", playableMovies.size(), duration);

        return playableMovies;
    }

    // ==================== SECTION 2: Tabs ====================

    @Cacheable(value = "homepage", key = "'section2'")
    public HomepageResponse.Section2Data fetchSection2() {
        log.info("📥 Fetching Section 2...");
        long startTime = System.currentTimeMillis();

        List<MovieItemDTO> listKorea = listKoreaRaw();
        List<MovieItemDTO> listChina = listChinaRaw();
        List<MovieItemDTO> listUSUK = listUSUKRaw();

        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ Section 2: Korea={}, China={}, USUK={} in {}ms",
                listKorea.size(), listChina.size(), listUSUK.size(), duration);

        return HomepageResponse.Section2Data.builder()
                .ListKorea(listKorea)
                .ListChina(listChina)
                .ListUSAndUK(listUSUK)
                .build();
    }

    public List<MovieItemDTO> listKoreaRaw() {
        Map<String, String> params = new HashMap<>();
        params.put("country", "han-quoc");
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        return fetchListSectionWithFilter("phim-moi", params, DEFAULT_LIMIT);
    }

    public List<MovieItemDTO> listChinaRaw() {
        Map<String, String> params = new HashMap<>();
        params.put("country", "trung-quoc");
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        return fetchListSectionWithFilter("phim-moi", params, DEFAULT_LIMIT);
    }

    public List<MovieItemDTO> listUSUKRaw() {
        Map<String, String> params = new HashMap<>();
        params.put("country", "au-my");
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        return fetchListSectionWithFilter("phim-moi", params, DEFAULT_LIMIT);
    }

    // ==================== SECTIONS 3-5, 7-8, 10-12 ====================

    @Cacheable(value = "homepage", key = "'section3'")
    public List<MovieItemDTO> fetchSection3() {
        log.info("📥 Fetching Section 3 (Series)...");
        Map<String, String> params = new HashMap<>();
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        return fetchListSectionWithFilter("phim-bo", params, DEFAULT_LIMIT);
    }

    @Cacheable(value = "homepage", key = "'section4'")
    public List<MovieItemDTO> fetchSection4() {
        log.info("📥 Fetching Section 4 (Action)...");
        Map<String, String> params = new HashMap<>();
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("category", "hanh-dong");
        return fetchListSectionWithFilter("phim-chieu-rap", params, DEFAULT_LIMIT);
    }

    @Cacheable(value = "homepage", key = "'section5'")
    public List<MovieItemDTO> fetchSection5() {
        log.info("📥 Fetching Section 5 (Single)...");
        Map<String, String> params = new HashMap<>();
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        return fetchListSectionWithFilter("phim-le", params, DEFAULT_LIMIT);
    }

    // ==================== ✅ SECTION 6: FIX DUPLICATE ISSUE ====================

    @Cacheable(value = "homepage", key = "'section6'")
    public List<MovieItemDTO> fetchSection6() {
        log.info("📥 Fetching Section 6 (14 PLAYABLE items, skip Section 1 used)...");
        long startTime = System.currentTimeMillis();

        OphimHomepageResponse rawData = fetchHomepageRaw();
        List<OphimMovieItem> rawItems = rawData.getData().getItems();

        // Filter TRƯỚC, sau đó skip 7 items đã filter
        // Điều này đảm bảo skip đúng 7 items mà Section 1 đã dùng
        List<OphimMovieItem> preFilteredItems = rawItems.stream()
                .filter(isValidMovie())
                .skip(SECTION1_REQUIRED_COUNT)  // Skip 7 items ĐÃ FILTER
                .collect(Collectors.toList());

        log.info("📊 Section 6: {} items after filter + skip", preFilteredItems.size());

        // Fetch detail + check playable
        List<MovieItemDTO> playableMovies = fetchPlayableItemsParallel(preFilteredItems, SECTION6_REQUIRED_COUNT);

        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ Section 6: {} PLAYABLE movies in {}ms", playableMovies.size(), duration);

        return playableMovies;
    }

    @Cacheable(value = "homepage", key = "'section7'")
    public List<MovieItemDTO> fetchSection7() {
        log.info("📥 Fetching Section 7 (Horror)...");
        Map<String, String> params = new HashMap<>();
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("category", "kinh-di");
        return fetchListSectionWithFilter("phim-moi", params, DEFAULT_LIMIT);
    }

    @Cacheable(value = "homepage", key = "'section8'")
    public List<MovieItemDTO> fetchSection8() {
        log.info("📥 Fetching Section 8 (Korea Romance)...");
        Map<String, String> params = new HashMap<>();
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("category", "tinh-cam");
        params.put("country", "han-quoc");
        return fetchListSectionWithFilter("phim-moi", params, DEFAULT_LIMIT);
    }

    // ==================== SECTION 9: Cartoon ====================

    @Cacheable(value = "homepage", key = "'section9'")
    public List<MovieItemDTO> fetchSection9() {
        log.info("📥 Fetching Section 9 (Cartoon PLAYABLE)...");
        long startTime = System.currentTimeMillis();

        Map<String, String> params = new HashMap<>();
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("country", "nhat-ban,han-quoc");

        // Fetch raw với filter cho animation (KHÔNG filter type hoathinh)
        List<OphimMovieItem> preFilteredItems = fetchRawItemsGeneric(
                "hoat-hinh",
                params,
                DEFAULT_LIMIT * 2,
                isValidAnimation()
        );

        // Fetch detail + check playable
        List<MovieItemDTO> playableMovies = fetchPlayableItemsParallel(preFilteredItems, DEFAULT_LIMIT);

        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ Section 9: {} PLAYABLE animations in {}ms", playableMovies.size(), duration);

        return playableMovies;
    }

    @Cacheable(value = "homepage", key = "'section10'")
    public List<MovieItemDTO> fetchSection10() {
        log.info("📥 Fetching Section 10 (Crime)...");
        Map<String, String> params = new HashMap<>();
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("category", "hinh-su");
        return fetchListSectionWithFilter("phim-moi", params, DEFAULT_LIMIT);
    }

    @Cacheable(value = "homepage", key = "'section11'")
    public List<MovieItemDTO> fetchSection11() {
        log.info("📥 Fetching Section 11 (Mystery)...");
        Map<String, String> params = new HashMap<>();
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("category", "bi-an");
        return fetchListSectionWithFilter("phim-moi", params, DEFAULT_LIMIT);
    }

    @Cacheable(value = "homepage", key = "'section12'")
    public List<MovieItemDTO> fetchSection12() {
        log.info("📥 Fetching Section 12 (Adventure)...");
        Map<String, String> params = new HashMap<>();
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("category", "phieu-luu");
        return fetchListSectionWithFilter("phim-moi", params, DEFAULT_LIMIT);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Fetch list với filter cho sections thường (không check playable)
     */
    private List<MovieItemDTO> fetchListSectionWithFilter(String slug, Map<String, String> baseParams, int requiredCount) {
        List<OphimMovieItem> validItems = fetchRawItemsGeneric(slug, baseParams, requiredCount, isValidMovie());
        return validItems.stream()
                .map(this::processItemWithoutDetail)
                .collect(Collectors.toList());
    }

    /**
     * Generic fetch method với custom filter predicate
     */
    private List<OphimMovieItem> fetchRawItemsGeneric(
            String slug,
            Map<String, String> baseParams,
            int requiredCount,
            Predicate<OphimMovieItem> filterPredicate
    ) {
        List<OphimMovieItem> collectedItems = new ArrayList<>();
        int currentPage = 1;
        int fetchLimit = requiredCount * FETCH_MULTIPLIER;

        while (collectedItems.size() < requiredCount && currentPage <= MAX_PAGES) {
            try {
                Map<String, String> params = new HashMap<>(baseParams);
                params.put("page", String.valueOf(currentPage));
                params.put("limit", String.valueOf(fetchLimit));

                OphimListResponse response = ophimClient.getListBySlug(slug, params);

                if (response == null || response.getData() == null ||
                        response.getData().getItems() == null || response.getData().getItems().isEmpty()) {
                    log.debug("⚠️ No more data at page {}", currentPage);
                    break;
                }

                List<OphimMovieItem> items = response.getData().getItems();

                // Dùng custom filter predicate
                List<OphimMovieItem> validItems = items.stream()
                        .filter(filterPredicate)
                        .collect(Collectors.toList());

                log.debug("📊 {} page {}: {} raw → {} valid", slug, currentPage, items.size(), validItems.size());

                collectedItems.addAll(validItems);

                if (items.size() < fetchLimit) {
                    break;
                }
                currentPage++;

            } catch (Exception e) {
                log.error("❌ Error fetching {} page {}: {}", slug, currentPage, e.getMessage());
                break;
            }
        }

        List<OphimMovieItem> result = collectedItems.stream()
                .limit(requiredCount)
                .collect(Collectors.toList());

        log.info("📊 {} - Got {} items (required: {})", slug, result.size(), requiredCount);

        return result;
    }

    /**
     * Fetch detail + check playable cho list items (parallel)
     */
    private List<MovieItemDTO> fetchPlayableItemsParallel(List<OphimMovieItem> candidates, int requiredCount) {
        log.debug("🚀 Checking playable for {} candidates (need {})...", candidates.size(), requiredCount);

        List<CompletableFuture<MovieItemDTO>> futures = candidates.stream()
                .map(item -> CompletableFuture.supplyAsync(
                        () -> processItemWithDetailAndPlayableCheck(item, true),
                        executorService
                ))
                .collect(Collectors.toList());

        List<MovieItemDTO> result = futures.stream()
                .map(future -> {
                    try {
                        return future.join();
                    } catch (Exception e) {
                        log.error("❌ Future failed: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .limit(requiredCount)
                .collect(Collectors.toList());

        log.debug("📊 Got {} playable out of {} candidates", result.size(), candidates.size());

        return result;
    }

    /**
     * Process item: Fetch detail + Check playable + Build DTO
     *
     * @return MovieItemDTO nếu playable, NULL nếu không
     */
    private MovieItemDTO processItemWithDetailAndPlayableCheck(OphimMovieItem item, boolean requirePoster) {
        try {
            OphimMovieDetailResponse detailResponse = ophimClient.getMovieDetail(item.getSlug());

            if (detailResponse == null || detailResponse.getData() == null
                    || detailResponse.getData().getItem() == null) {
                log.debug("⚠️ Empty detail for: {}", item.getSlug());
                return null;
            }

            OphimMovieDetail detailItem = detailResponse.getData().getItem();

            // CHECK PLAYABLE
            if (!hasPlayableEpisodes(detailItem)) {
                log.debug("⏭️ SKIP {} - no playable episodes", item.getSlug());
                return null;
            }

            log.debug("✅ KEEP {} - has playable episodes", item.getSlug());

            // Build DTO
            MovieItemDTO dto = new MovieItemDTO();
            BeanUtils.copyProperties(item, dto);

            dto.setContent(detailItem.getContent());

            String posterUrl = (detailItem.getPosterUrl() != null && !detailItem.getPosterUrl().isEmpty())
                    ? detailItem.getPosterUrl()
                    : item.getPosterUrl();
            String thumbUrl = (detailItem.getThumbUrl() != null && !detailItem.getThumbUrl().isEmpty())
                    ? detailItem.getThumbUrl()
                    : item.getThumbUrl();

            if (requirePoster && (posterUrl == null || posterUrl.isEmpty())) return null;

            dto.setPosterUrl(posterUrl);
            dto.setThumbUrl(thumbUrl);
            dto.setOptimizedThumb(imageService.optimizeThumb(thumbUrl, item.getSlug()));
            dto.setOptimizedPoster(imageService.optimizedPoster(posterUrl, item.getSlug()));

            return dto;

        } catch (Exception e) {
            log.error("❌ Error processing {}: {}", item.getSlug(), e.getMessage());
            return null;
        }
    }

    /**
     * Process item không cần check playable (cho sections 2-5, 7-8, 10-12)
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