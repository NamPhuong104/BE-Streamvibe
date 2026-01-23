package movieapp.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.dto.Cache.CacheActionResponse;
import movieapp.dto.Cache.CacheDetailResponse;
import movieapp.dto.Cache.CacheListResponse;
import movieapp.dto.Cache.CacheStatsDTO;
import movieapp.service.CacheService;
import movieapp.util.annotation.ApiMessage;
import movieapp.util.annotation.RequireAdmin;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/cache")
@Slf4j
@RequireAdmin
@RequiredArgsConstructor
public class CacheController {
    private final CacheService cacheService;

    // ==================== LIST & STATS ====================

    /**
     * Lấy danh sách cache với filter và pagination
     * <p>
     * GET /admin/cache?page=1&size=10&group=initial&status=CACHED&search=section1
     */
    @GetMapping
    @ApiMessage("Fetch cache list successfully")
    public CacheListResponse listCache(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        log.info("📊 GET /admin/cache - page: {}, size: {}, group: {}, status: {}, search: {}",
                page, size, group, status, search);
        return cacheService.listCache(page, size, group, status, search);
    }

    /**
     * Lấy thống kê tổng quan cache
     * <p>
     * GET /admin/cache/stats
     */
    @GetMapping("/stats")
    @ApiMessage("Fetch cache stats successfully")
    public CacheStatsDTO getStats() {
        log.info("📊 GET /admin/cache/stats");
        return cacheService.getStats();
    }

    /**
     * Lấy chi tiết một cache key (bao gồm data)
     * <p>
     * GET /admin/cache/section1
     */
    @GetMapping("/{section}")
    @ApiMessage("Fetch cache detail successfully")
    public CacheDetailResponse getCacheDetail(@PathVariable String section) {
        log.info("📊 GET /admin/cache/{}", section);
        CacheDetailResponse response = cacheService.getCacheDetail(section);

        return response;
    }

    // ==================== REFRESH ACTIONS ====================

    /**
     * Refresh một section cụ thể
     * <p>
     * POST /admin/cache/refresh/section1
     */
    @PostMapping("/refresh/{section}")
    @ApiMessage("Section refreshed successfully")
    public CacheActionResponse refreshSection(@PathVariable String section) {
        log.info("🔄 POST /admin/cache/refresh/{}", section);

        return cacheService.refreshSection(section);
    }

    /**
     * Refresh một group
     * <p>
     * POST /admin/cache/refresh/group/initial
     */
    @PostMapping("/refresh/group/{group}")
    @ApiMessage("Group refreshed successfully")
    public CacheActionResponse refreshGroup(@PathVariable String group) {
        log.info("🔄 POST /admin/cache/refresh/group/{}", group);
        return cacheService.refreshGroup(group);
    }

    /**
     * Refresh tất cả cache
     * <p>
     * POST /admin/cache/refresh/all
     */
    @PostMapping("/refresh/all")
    @ApiMessage("All cache refreshed successfully")
    public CacheActionResponse refreshAll() {
        log.info("🔄 POST /admin/cache/refresh/all");
        return cacheService.refreshAll();
    }

    // ==================== CLEAR ACTIONS ====================

    /**
     * Clear một section cụ thể
     * <p>
     * DELETE /admin/cache/section1
     */
    @DeleteMapping("/{section}")
    @ApiMessage("Section cleared successfully")
    public CacheActionResponse clearSection(@PathVariable String section) {
        log.info("🗑️ DELETE /admin/cache/{}", section);
        return cacheService.clearSection(section);
    }

    /**
     * Clear một group
     * <p>
     * DELETE /admin/cache/group/initial
     */
    @DeleteMapping("/group/{group}")
    @ApiMessage("Group cleared successfully")
    public CacheActionResponse clearGroup(@PathVariable String group) {
        log.info("🗑️ DELETE /admin/cache/group/{}", group);
        return cacheService.clearGroup(group);
    }

    /**
     * Clear tất cả cache
     * <p>
     * DELETE /admin/cache/all
     */
    @DeleteMapping("/all")
    @ApiMessage("All cache cleared successfully")
    public CacheActionResponse clearAll() {
        log.info("🗑️ DELETE /admin/cache/all");
        return cacheService.clearAll();
    }
}
