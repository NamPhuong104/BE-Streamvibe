package movieapp.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.dto.MovieDetail.AggregatedMovieDetailResponse;
import movieapp.dto.OphimResponse.OphimListResponse;
import movieapp.entity.User;
import movieapp.service.MovieAggregationService;
import movieapp.service.MovieProxyService;
import movieapp.service.UserService;
import movieapp.util.SecurityUtil;
import movieapp.util.annotation.ApiMessage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
@Slf4j
public class MovieController {
    private final MovieProxyService movieProxyService;
    private final MovieAggregationService movieAggregationService;
    private final UserService userService;

    @GetMapping("/{slug}/detail")
    @ApiMessage("Fetch movie detail")
    public AggregatedMovieDetailResponse getMovieDetail(@PathVariable String slug) {
        User currentUser = getCurrentUserOrNull();

        try {
            AggregatedMovieDetailResponse response = movieAggregationService.getAggregatedMovieDetail(slug, currentUser);

            if (response.getMovieData() == null || response.getMovieData() == null || response.getMovieData() == null)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy phim: " + slug);

            return response;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch movie detail"
            );
        }
    }


    // ==================== SEARCH ====================

    @GetMapping("/search")
    @ApiMessage("Tìm kiếm phim thành công")
    public OphimListResponse.ListData search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int limit) {

        return movieProxyService.search(keyword, page, limit);
    }

    // ==================== DANH-SACH ====================

    @GetMapping("/danh-sach/{slug}")
    @ApiMessage("Lấy danh sách phim thành công")
    public OphimListResponse.ListData getListBySlug(
            @PathVariable String slug,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int limit,
            @RequestParam(required = false) String sort_field,
            @RequestParam(required = false) String sort_type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String year) {

        Map<String, String> params = buildParams(page, limit, sort_field, sort_type, category, country, year);
        return movieProxyService.getListBySlug(slug, params);
    }

    // ==================== THE-LOAI ====================

    @GetMapping("/the-loai/{slug}")
    @ApiMessage("Lấy phim theo thể loại thành công")
    public OphimListResponse.ListData getByCategory(
            @PathVariable String slug,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int limit,
            @RequestParam(required = false) String sort_field,
            @RequestParam(required = false) String sort_type,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String year) {

        Map<String, String> params = buildParams(page, limit, sort_field, sort_type, null, country, year);
        return movieProxyService.getByCategory(slug, params);
    }

    // ==================== QUOC-GIA ====================

    @GetMapping("/quoc-gia/{slug}")
    @ApiMessage("Lấy phim theo quốc gia thành công")
    public OphimListResponse.ListData getByCountry(
            @PathVariable String slug,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "24") int limit,
            @RequestParam(required = false) String sort_field,
            @RequestParam(required = false) String sort_type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String year) {

        Map<String, String> params = buildParams(page, limit, sort_field, sort_type, category, null, year);
        return movieProxyService.getByCountry(slug, params);
    }

    // ==================== HELPER ====================

    private Map<String, String> buildParams(int page, int limit, String sortField, String sortType,
                                            String category, String country, String year) {
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("limit", String.valueOf(limit));

        if (sortField != null && !sortField.isEmpty()) params.put("sort_field", sortField);
        if (sortType != null && !sortType.isEmpty()) params.put("sort_type", sortType);
        if (category != null && !category.isEmpty()) params.put("category", category);
        if (country != null && !country.isEmpty()) params.put("country", country);
        if (year != null && !year.isEmpty()) params.put("year", year);

        return params;
    }

    private User getCurrentUserOrNull() {
        try {
            String email = SecurityUtil.getCurrentUserLogin().orElse(null);
            if (email == null || email.isEmpty()) return null;

            return userService.handleFindUserByEmailOrUsername(email);
        } catch (Exception e) {
            return null;
        }
    }
}
