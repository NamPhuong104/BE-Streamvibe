package movieapp.service;

import lombok.extern.slf4j.Slf4j;
import movieapp.client.OphimClient;
import movieapp.dto.HomepageReponse.HomepageResponse;
import movieapp.dto.OphimResponse.OphimHomepageResponse;
import movieapp.dto.OphimResponse.OphimListResponse;
import movieapp.dto.OphimResponse.OphimMovieDetailResponse;
import movieapp.dto.OphimResponse.OphimMovieItem;
import movieapp.dto.CustomFieldsResponse.MovieItemDTO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CachedSectionService {
    private final OphimClient ophimClient;
    private final ImageOptimizationService imageService;
    private final ExecutorService executorService;

    public CachedSectionService(OphimClient ophimClient, ImageOptimizationService imageService, @Qualifier("taskExecutor") ExecutorService executorService) {
        this.ophimClient = ophimClient;
        this.imageService = imageService;
        this.executorService = executorService;
    }

    //    FETCH RAW OPHIM
    @Cacheable(value = "homepage", key = "'raw'")
    public OphimHomepageResponse fetchHomepageRaw() {
        log.info("📥 Fetching raw homepage data from Ophim...");
        return ophimClient.getHomepage();
    }


    @Cacheable(value = "homepage", key = "'section1'")
    public List<MovieItemDTO> fetchSection1() {
        log.info("📥 Fetching Section 1 (7 items with POSTER + CONTENT - PARALLEL)...");
        OphimHomepageResponse rawData = fetchHomepageRaw();
        List<OphimMovieItem> rawItems = rawData.getData().getItems();

        List<OphimMovieItem> first7Items = rawItems.stream().limit(7).collect(Collectors.toList());

        return fetchItemsWithDetailParallel(first7Items);
    }

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

    //    FETCH SECTION 2 LIST KOREA
    public List<MovieItemDTO> listKoreaRaw() {
        log.info("📥 Fetching Section 2 List Korea...");

        Map<String, String> params = new HashMap<>();
        params.put("country", "han-quoc");
        params.put("page", "1");
        params.put("limit", "14");
        params.put("sort_field", "year");
        params.put("sort_type", "desc");

        return fetchListSection("phim-moi", params);
    }

    //    FETCH SECTION 2 LIST CHINA
    public List<MovieItemDTO> listChinaRaw() {
        log.info("📥 Fetching Section 2 List China...");

        Map<String, String> params = new HashMap<>();
        params.put("country", "trung-quoc");
        params.put("page", "1");
        params.put("limit", "14");
        params.put("sort_field", "year");
        params.put("sort_type", "desc");

        return fetchListSection("phim-moi", params);
    }

    //    FETCH SECTION 2 US/UK
    public List<MovieItemDTO> listUSUKRaw() {
        log.info("📥 Fetching Section 2 List US/UK...");

        Map<String, String> params = new HashMap<>();
        params.put("country", "au-my");
        params.put("page", "1");
        params.put("limit", "14");
        params.put("sort_field", "year");
        params.put("sort_type", "desc");

        return fetchListSection("phim-moi", params);
    }


    //    FETCH SECTION 3 SERIES
    @Cacheable(value = "homepage", key = "'section3'")
    public List<MovieItemDTO> fetchSection3() {
        log.info("📥 Fetching Section 3 List Series...");

        Map<String, String> params = new HashMap<>();
        params.put("page", "1");
        params.put("limit", "14");
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("year", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));

        return fetchListSection("phim-bo", params);
    }

    //    FETCH SECTION 4 ACTION
    @Cacheable(value = "homepage", key = "'section4'")
    public List<MovieItemDTO> fetchSection4() {
        log.info("📥 Fetching Section 4 List Action...");

        Map<String, String> params = new HashMap<>();
        params.put("page", "1");
        params.put("limit", "14");
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("category", "hanh-dong");

        return fetchListSection("phim-chieu-rap", params);
    }

    //    FETCH SECTION 5 SINGLE
    @Cacheable(value = "homepage", key = "'section5'")
    public List<MovieItemDTO> fetchSection5() {
        log.info("📥 Fetching Section 5 List Single...");

        Map<String, String> params = new HashMap<>();
        params.put("page", "1");
        params.put("limit", "14");
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("year", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));

        return fetchListSection("phim-le", params);
    }

    @Cacheable(value = "homepage", key = "'section6'")
    public List<MovieItemDTO> fetchSection6() {
        log.info("📥 Fetching Section 6 (14 items WITHOUT detail)...");
        OphimHomepageResponse rawData = fetchHomepageRaw();
        List<OphimMovieItem> rawItems = rawData.getData().getItems();

        return rawItems.stream().skip(7).limit(14).map(this::processItemWithoutDetail).collect(Collectors.toList());
    }

    //    FETCH SECTION 7 HORROR
    @Cacheable(value = "homepage", key = "'section7'")
    public List<MovieItemDTO> fetchSection7() {
        log.info("📥 Fetching Section 7 List HORROR...");

        Map<String, String> params = new HashMap<>();
        params.put("page", "1");
        params.put("limit", "14");
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("category", "kinh-di");
        params.put("country", "thai-lan");

        return fetchListSection("phim-moi", params);
    }

    //    FETCH SECTION 8 KOREA LOVE
    @Cacheable(value = "homepage", key = "'section8'")
    public List<MovieItemDTO> fetchSection8() {
        log.info("📥 Fetching Section 8 List Korea Love...");

        Map<String, String> params = new HashMap<>();
        params.put("page", "1");
        params.put("limit", "14");
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("category", "tinh-cam");
        params.put("country", "han-quoc");

        return fetchListSection("phim-moi", params);
    }

    //    FETCH SECTION 9 CARTOON (WITH CONTENT)
    @Cacheable(value = "homepage", key = "'section9'")
    public List<MovieItemDTO> fetchSection9() {
        log.info("📥 Fetching Section 9 (Cartoon)...");

        Map<String, String> params = new HashMap<>();
        params.put("page", "1");
        params.put("limit", "14");
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("country", "nhat-ban, han-quoc");

        OphimListResponse response = ophimClient.getListBySlug("hoat-hinh", params);
        List<OphimMovieItem> items = response.getData().getItems();

        return fetchItemsWithDetailParallel(items.stream().limit(14).collect(Collectors.toList()));
    }

    //    FETCH SECTION 10 CRIME
    @Cacheable(value = "homepage", key = "'section10'")
    public List<MovieItemDTO> fetchSection10() {
        log.info("📥 Fetching Section 10 List Crime...");

        Map<String, String> params = new HashMap<>();
        params.put("page", "1");
        params.put("limit", "14");
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("category", "hinh-su");

        return fetchListSection("phim-moi", params);
    }

    //    FETCH SECTION 11 SECRET
    @Cacheable(value = "homepage", key = "'section11'")
    public List<MovieItemDTO> fetchSection11() {
        log.info("📥 Fetching Section 11 List Secret...");

        Map<String, String> params = new HashMap<>();
        params.put("page", "1");
        params.put("limit", "14");
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("category", "bi-an");

        return fetchListSection("phim-moi", params);
    }

    //    FETCH SECTION 12 ADVENTURE
    @Cacheable(value = "homepage", key = "'section12'")
    public List<MovieItemDTO> fetchSection12() {
        log.info("📥 Fetching Section 12 List Adventure...");

        Map<String, String> params = new HashMap<>();
        params.put("page", "1");
        params.put("limit", "14");
        params.put("sort_field", "year");
        params.put("sort_type", "desc");
        params.put("category", "phieu-luu");

        return fetchListSection("phim-moi", params);
    }

    //  HELPER: FETCH LIST SECTION (NO CONTENT)
    private List<MovieItemDTO> fetchListSection(String slug, Map<String, String> params) {
        OphimListResponse response = ophimClient.getListBySlug(slug, params);
        List<OphimMovieItem> items = response.getData().getItems();

        return items.stream()
                .limit(14)
                .map(this::processItemWithoutDetail)
                .collect(Collectors.toList());
    }

    //    PARALLEL FETCH DETAIL
    private List<MovieItemDTO> fetchItemsWithDetailParallel(List<OphimMovieItem> items) {
        log.info("🚀 Fetching detail for {} items in PARALLEL...", items.size());
        List<CompletableFuture<MovieItemDTO>> futures = items.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> processItemWithDetail(item), executorService))
                .collect(Collectors.toList());

        return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    //    HELPER: GET MOVIE DETAIL
    private MovieItemDTO processItemWithDetail(OphimMovieItem item) {
        MovieItemDTO dto = new MovieItemDTO();
        BeanUtils.copyProperties(item, dto);

        try {
            log.debug("📄 Fetching content for: {}", item.getSlug());
            OphimMovieDetailResponse detailResponse = ophimClient.getMovieDetail(item.getSlug());
            dto.setContent(detailResponse.getData().getItem().getContent());
            dto.setOptimizedThumb(imageService.optimizeThumb(detailResponse.getData().getItem().getThumbUrl(), detailResponse.getData().getItem().getSlug()));
            dto.setOptimizedPoster(imageService.optimizedPoster(detailResponse.getData().getItem().getPosterUrl(), detailResponse.getData().getItem().getSlug()));
        } catch (Exception e) {
            log.warn("⚠️ Failed to fetch content for {}: {}", item.getSlug(), e.getMessage());
            dto.setContent(null);
        }
        return dto;
    }

    //    HELPER: HANDLE ITEM NO CONTENT
    private MovieItemDTO processItemWithoutDetail(OphimMovieItem item) {
        MovieItemDTO dto = new MovieItemDTO();
        BeanUtils.copyProperties(item, dto);

        dto.setOptimizedThumb(imageService.optimizeThumb(item.getThumbUrl(), item.getSlug()));
        dto.setOptimizedPoster(imageService.optimizedPoster(item.getPosterUrl(), item.getSlug()));
        dto.setContent(null);

        return dto;
    }
}
