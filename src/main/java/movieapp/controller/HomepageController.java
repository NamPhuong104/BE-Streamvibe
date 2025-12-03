package movieapp.controller;

import lombok.extern.slf4j.Slf4j;
import movieapp.dto.HomepageReponse.HomepageGroupResponse;
import movieapp.dto.HomepageReponse.HomepageResponse;
import movieapp.service.HomepageService;
import movieapp.service.CacheStrategy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@CrossOrigin(origins = "*")
@Slf4j
public class HomepageController {
    private final HomepageService homepageService;
    private final CacheStrategy cacheStrategy;
    private final RedisTemplate<String, Object> redisTemplate;

    public HomepageController(HomepageService homepageService, CacheStrategy cacheStrategy, RedisTemplate<String, Object> redisTemplate) {
        this.homepageService = homepageService;
        this.cacheStrategy = cacheStrategy;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping(value = {"/homepage", "/homepage/{group}"})
    public ResponseEntity<HomepageGroupResponse> getHomepageGrouped(
            @PathVariable(required = false) String group) {

        String resolvedGroup = (group == null || group.isBlank()) ? "initial" : group.toLowerCase();

        if (!List.of("initial", "group1", "group2").contains(group.toLowerCase())) {
            log.error("❌ Invalid group requested: {}", group);
            throw new IllegalArgumentException("Invalid group. Must be: initial, group1, or group2");
        }

        log.info("📥 Received request for homepage group: {}", group);
        long startTime = System.currentTimeMillis();

        HomepageGroupResponse response = homepageService.getHomepageByGroup(resolvedGroup);

        long duration = System.currentTimeMillis() - startTime;
        log.info("📤 Returning group {} in {}ms", group, duration);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/fullHomepage")
    public ResponseEntity<?> getHomepage() {
        log.info("📥 Received request for FULL homepage data");
        HomepageResponse response = homepageService.getHomepageData();
        return ResponseEntity.ok(response);
    }


    //    ====== ADMIN ENDPOINTS ======

    //    ====== REFRESH CACHE ENDPOINTS ======
    //   MANUAL REFRESH ALL CACHE
    @PostMapping("/admin/refresh/new")
    public ResponseEntity<Map<String, Object>> manualRefresh() {
        log.warn("🚨 Admin manual refresh requested");

        try {
            long startTime = System.currentTimeMillis();
            cacheStrategy.manualRefreshAll();
            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "All cache refreshed successfully");
            response.put("duration_ms", duration);
            response.put("timestamp", new Date());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Failed to refresh: {}", e.getMessage());

            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            error.put("timestamp", new Date());

            return ResponseEntity.internalServerError().body(error);
        }
    }

    //   MANUAL REFRESH CACHE BY SECTION
    @PostMapping("/admin/refresh/section/{section}")
    public ResponseEntity<Map<String, Object>> manualRefreshSection(@PathVariable("section") String section) {
        log.warn("🚨 Admin manual refresh requested for section: {}", section);

        try {
            long startTime = System.currentTimeMillis();
            cacheStrategy.manualRefreshSection(section);
            long duration = System.currentTimeMillis();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Section " + section + " refreshed successfully",
                    "section", section,
                    "duration_ms", duration,
                    "timestamp", new Date()
            ));
        } catch (Exception e) {
            log.warn("🚨 Error Admin manual refresh requested for section: {}", e.getMessage());
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            error.put("timestamp", new Date());

            return ResponseEntity.internalServerError().body(error);
        }
    }

    //   MANUAL REFRESH CACHE BY GROUP
    @PostMapping("/admin/refresh/group/{group}")
    public ResponseEntity<Map<String, Object>> manualRefreshGroup(@PathVariable("group") String group) {
        log.warn("🚨 Admin manual refresh requested for group: {}", group);

        try {
            long startTime = System.currentTimeMillis();
            cacheStrategy.manualRefreshGroup(group);
            long duration = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Group " + group + " refreshed successfully",
                    "group", group,
                    "duration_ms", duration,
                    "timestamp", new Date()
            ));
        } catch (Exception e) {
            log.error("❌ Failed to refresh group {}: {}", group, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "timestamp", new Date()
            ));
        }
    }

    //    ====== CLEAR CACHE ENDPOINTS ======
    //    MANUAL CLEAR CACHE BY SECTION KEY
    @DeleteMapping("/admin/cache/section/{section}")
    public ResponseEntity<Map<String, Object>> clearCacheBySectionKey(@PathVariable("section") String section) {
        log.warn("🚨 Admin manual clear cache by section: {}", section);
        try {
            long startTime = System.currentTimeMillis();
            cacheStrategy.clearSectionCache(section);
            long duration = System.currentTimeMillis();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Section " + section + " refreshed successfully",
                    "section", section,
                    "duration_ms", duration,
                    "timestamp", new Date()
            ));
        } catch (Exception e) {
            log.warn("🚨 Error Admin manual refresh requested for section: {}", e.getMessage());
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            error.put("timestamp", new Date());

            return ResponseEntity.internalServerError().body(error);
        }
    }

    @DeleteMapping("/admin/cache/group/{group}")
    public ResponseEntity<Map<String, Object>> clearCacheByGroup(@PathVariable("group") String group) {
        log.warn("🚨 Admin manual clear cache by group: {}", group);
        try {
            long startTIme = System.currentTimeMillis();
            cacheStrategy.manualClearCacheByGroup(group);
            long duration = System.currentTimeMillis() - startTIme;

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Group " + group + " cleared successfully",
                    "group", group,
                    "duration_ms", duration,
                    "timestamp", new Date()
            ));
        } catch (Exception e) {
            log.warn("🚨 Error Admin manual clear cache requested for group: {}", e.getMessage());
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(error);
        }
    }

    //      MANUAL CLEAR ALL CACHE
    @DeleteMapping("/admin/cache/clearAll")
    public ResponseEntity<Map<String, Object>> clearCache() {
        log.warn("🗑️ Admin clear cache requested");

        cacheStrategy.clearAllCache();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "All cache cleared");
        response.put("warning", "Data will be fetched on next request");
        response.put("timestamp", new Date());

        return ResponseEntity.ok(response);
    }


    //    ====== STATUS CACHE ENDPOINTS ======
    //    CHECK STATUS CACHE
    @GetMapping("/admin/cache/status")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        log.info("📊 Cache status requested");
        Map<String, Object> status = new LinkedHashMap<>();

        Set<String> keys = redisTemplate.keys("homepage::*");

        status.put("timestamp", new Date());
        status.put("total_Keys", keys != null ? keys.size() : 0);

        //        Detail key
        if (keys != null && !keys.isEmpty()) {
            Map<String, Object> keyDetails = new LinkedHashMap<>();

            for (String key : keys) {
                Long ttlSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);

                Map<String, Object> detail = new LinkedHashMap<>();
                if (ttlSeconds != null && ttlSeconds > 0) {
                    detail.put("status", "CACHED");
                    detail.put("ttl_seconds", ttlSeconds);
                    detail.put("ttl_minutes", ttlSeconds / 60);
                    detail.put("expires_at", new Date(System.currentTimeMillis() + (ttlSeconds * 1000)));
                } else {
                    detail.put("status", "EXPIRED");
                    detail.put("ttl_seconds", 0);
                }
                keyDetails.put(key, detail);
            }
            status.put("keys", keyDetails);
        } else {
            status.put("warning", "No cache found. Cache will be created on next request or scheduled refresh.");
        }
        return ResponseEntity.ok(status);
    }


    //    ====== INFO CONFIG AND ENDPOINTS ======
    //    INFO CONFIG
    @GetMapping("/admin/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = new LinkedHashMap<>();

        info.put("app_name", "Movie Backend");
        info.put("version", "2.0.0"); // Update version
        info.put("cache_strategy", "Grouped Refresh (3 groups)");
        info.put("cache_ttl_minutes", 60);
        info.put("warm_up_delay_seconds", 10);

        Map<String, String> groups = new LinkedHashMap<>();
        groups.put("initial", "Sections 1-4 (Homepage initial load)");
        groups.put("group1", "Sections 5-8 (Load on scroll)");
        groups.put("group2", "Sections 9-12 (Load on scroll)");
        info.put("groups", groups);

        Map<String, String> refreshSchedule = new LinkedHashMap<>();
        refreshSchedule.put("minute_54", "Initial group (1-4)");
        refreshSchedule.put("minute_55", "Group 1 (5-8)");
        refreshSchedule.put("minute_56", "Group 2 (9-12)");
        info.put("refresh_schedule", refreshSchedule);

        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("homepage_grouped", "GET /api/v1/homepage/grouped?group={initial|group1|group2}");
        endpoints.put("homepage_legacy", "GET /api/v1/fullHomepage");
        endpoints.put("refresh_group", "POST /api/v1/admin/refresh/group/{groupName}");
        endpoints.put("refresh_all", "POST /api/v1/admin/refresh");
        endpoints.put("cache_status", "GET /api/v1/admin/cache/status");
        info.put("endpoints", endpoints);

        info.put("timestamp", new Date());
        return ResponseEntity.ok(info);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> checkHealth() {
        return ResponseEntity.ok(Map.of("status", "UP", "message", "Service is running"));
    }
}
