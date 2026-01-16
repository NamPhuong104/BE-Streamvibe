package movieapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.config.properties.CacheProperties;
import org.hibernate.Cache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Recover;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class CacheStrategy {
    private final HomepageService homepageService;
    private final CachedSectionService cachedSectionService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheProperties cache;

    // ===================================
    // INJECT CONFIGURATION VALUES
    // ===================================
    @Value("${app.cache.warmup-delay-seconds:30}")
    private int warmupDelaySeconds;

    @Value("${app.cache.ttl-minutes:60}")
    private int cacheTtlMinutes;

    @Value("${app.cache.schedule.enabled:true}")
    private boolean scheduleEnabled;


    // ===================================
    // WARM-UP ON STARTUP (Sử dụng config)
    // ===================================
    @Scheduled(initialDelayString = "${app.cache.warmup-delay-seconds}000", fixedRate = Long.MAX_VALUE)
    public void warmUpOnStartup() {
        log.info("🚀 ========================================");
        log.info("🚀 [STARTUP] Starting cache warm-up...");
        log.info("🚀 ========================================");

        long startTime = System.currentTimeMillis();

        try {
            homepageService.getHomepageData();
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ [STARTUP] Cache warmed up successfully!");
            log.info("✅ [STARTUP] Duration: {}ms ({}s)", duration, duration / 1000);
            log.info("✅ [STARTUP] Users can now access homepage instantly");
            log.info("========================================");
        } catch (Exception e) {
            log.error("❌ ========================================");
            log.error("❌ [STARTUP] Cache warm-up FAILED!");
            log.error("❌ Error: {}", e.getMessage(), e);
            log.error("❌ ========================================");
        }
    }

    // ===================================
    // SCHEDULED REFRESH - INITIAL GROUP (Sections 1-4)
    // ===================================
    @Scheduled(cron = "${app.cache.schedule.initial-group}")
    public void refreshInitialGroup() {
        if (!scheduleEnabled) {
            log.debug("⏭️ Scheduled refresh is disabled");
            return;
        }

        log.info("⏰ ========================================");
        log.info("⏰ [MINUTE 54] Refreshing INITIAL group (sections 1-4)");
        log.info("⏰ ========================================");

        long startTime = System.currentTimeMillis();

        try {
            refreshGroupSafely("initial", List.of("raw", "section1", "section2", "section3", "section4"), () -> {
                cachedSectionService.fetchHomepageRaw();
                cachedSectionService.fetchSection1();
                cachedSectionService.fetchSection2();
                cachedSectionService.fetchSection3();
                cachedSectionService.fetchSection4();
            });

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ [MINUTE 54] Initial group refreshed in {}ms ({}s)", duration, duration / 1000);
            log.info("========================================");
        } catch (Exception e) {
            log.error("❌ [MINUTE 54] Failed to refresh initial group: {}", e.getMessage());
        }
    }

    // ===================================
    // SCHEDULED REFRESH - GROUP 1 (Sections 5-8)
    // ===================================
    @Scheduled(cron = "${app.cache.schedule.group1}")
    public void refreshGroup1() {
        if (!scheduleEnabled) {
            log.debug("⏭️ Scheduled refresh is disabled");
            return;
        }

        log.info("⏰ ========================================");
        log.info("⏰ [MINUTE 55] Refreshing GROUP 1 (sections 5-8)");
        log.info("⏰ ========================================");

        long startTime = System.currentTimeMillis();

        try {
            refreshGroupSafely("group1", List.of("section5", "section6", "section7", "section8"), () -> {
                cachedSectionService.fetchHomepageRaw();
                cachedSectionService.fetchSection5();
                cachedSectionService.fetchSection6();
                cachedSectionService.fetchSection7();
                cachedSectionService.fetchSection8();
            });

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ [MINUTE 55] Group 1 refreshed in {}ms ({}s)", duration, duration / 1000);
            log.info("========================================");
        } catch (Exception e) {
            log.error("❌ [MINUTE 55] Failed to refresh Group 1: {}", e.getMessage());
        }
    }

    // ===================================
    // SCHEDULED REFRESH - GROUP 2 (Sections 9-12)
    // ===================================
    @Scheduled(cron = "${app.cache.schedule.group2}")
    public void refreshGroup2() {
        if (!scheduleEnabled) {
            log.debug("⏭️ Scheduled refresh is disabled");
            return;
        }

        log.info("⏰ ========================================");
        log.info("⏰ [MINUTE 56] Refreshing GROUP 2 (sections 9-12)");
        log.info("⏰ ========================================");

        long startTime = System.currentTimeMillis();

        try {
            refreshGroupSafely("group2", List.of("section9", "section10", "section11", "section12"), () -> {
                cachedSectionService.fetchSection9();
                cachedSectionService.fetchSection10();
                cachedSectionService.fetchSection11();
                cachedSectionService.fetchSection12();
            });

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ [MINUTE 56] Group 2 refreshed in {}ms ({}s)", duration, duration / 1000);
            log.info("========================================");
        } catch (Exception e) {
            log.error("❌ [MINUTE 56] Failed to refresh Group 2: {}", e.getMessage());
        }
    }

    // ===================================
    // ✅ CORE LOGIC: Fetch-Verify-Swap Pattern
    // ===================================
    public void refreshGroupSafely(String groupName, List<String> cacheKeys, Runnable fetchAction) {
        log.info("🔄 [{}] Starting safe refresh...", groupName);

        List<String> tempKeys = backupCacheToTemp(cacheKeys);
        try {
            clearSectionsCache(cacheKeys);
            log.info("🗑️ [{}] Cleared old cache", groupName);

            fetchAction.run();
            log.info("✅ [{}] Fetched new data successfully", groupName);

            verifyCacheExist(cacheKeys);
            log.info("✅ [{}] Verified new cache exists", groupName);

        } catch (Exception e) {
            log.error("❌ [{}] Fetch failed: {}", groupName, e.getMessage());

            // Step 6: Rollback - Restore old cache from temp
            restoreCacheFromTemp(cacheKeys, tempKeys);

            throw e;
        } finally {
            deleteTempBackup(tempKeys);
        }
    }

    // ===================================
    // BACKUP CACHE TO TEMP KEYS
    // ===================================
    private List<String> backupCacheToTemp(List<String> cacheKeys) {
        List<String> tempKeys = new ArrayList<>();

        for (String key : cacheKeys) {
            String redisKey = "homepage::" + key;
            String tempKey = "homepage::temp::" + key;

            Object oldValue = redisTemplate.opsForValue().get(redisKey);
            if (oldValue != null) {
                redisTemplate.opsForValue().set(tempKey, oldValue, 10, TimeUnit.MINUTES);
                tempKeys.add(tempKey);
                log.debug("📦 Backed up {} → {}", redisKey, tempKey);
            }
        }
        return tempKeys;
    }

    // ===================================
    // RESTORE CACHE FROM TEMP (ROLLBACK)
    // ===================================
    private void restoreCacheFromTemp(List<String> cacheKeys, List<String> tempKeys) {
        log.warn("🔄 Rolling back to old cache...");

        for (String key : cacheKeys) {
            String redisKey = "homepage::" + key;
            String tempKey = "homepage::temp::" + key;

            Object tempValue = redisTemplate.opsForValue().get(tempKey);
            if (tempValue != null) {
                redisTemplate.opsForValue().set(redisKey, tempValue, cache.getTtlMinutes(), TimeUnit.MINUTES);
                log.info("↩️ Restored {} from backup", redisKey);
            }
        }
    }

    // ===================================
    // DELETE TEMP BACKUP
    // ===================================
    private void deleteTempBackup(List<String> tempKeys) {
        for (String tempKey : tempKeys) {
            redisTemplate.delete(tempKey);
        }
        log.debug("🗑️ Deleted temp backup keys");
    }

    // ===================================
    // VERIFY CACHE EXISTS
    // ===================================
    private void verifyCacheExist(List<String> cacheKeys) {
        for (String key : cacheKeys) {
            String redisKeys = "homepage::" + key;
            Boolean exist = redisTemplate.hasKey(redisKeys);

            if (exist == null || !exist) {
                throw new IllegalStateException("Cache verification failed: " + redisKeys + " does not exist");
            }
        }
    }

    // ===================================
    // RECOVER METHOD (after 3 retries failed)
    // ===================================
    @Recover
    private void recoverFromRefreshFailure(Exception e, String groupName, List<String> cacheKeys, Runnable fetchAction) {
        log.error("❌ ========================================");
        log.error("❌ [{}] Refresh FAILED after 3 retries!", groupName);
        log.error("❌ Error: {}", e.getMessage());
        log.error("❌ Keeping OLD cache until next scheduled refresh");
        log.error("❌ ========================================");
    }

    public void manualRefreshAll() {
        log.warn("🚨 ========================================");
        log.warn("🚨 [MANUAL] Force refresh triggered by admin");
        log.warn("🚨 ========================================");

        long startTime = System.currentTimeMillis();

        try {
            // Clear & refresh
            clearAllCache();
            log.info("🗑️ [MANUAL] Cache cleared");
            homepageService.getHomepageData();

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ [MANUAL] Cache force refreshed successfully!");
            log.info("✅ [MANUAL] Duration: {}ms ({}s)", duration, duration / 1000);
            log.info("========================================");

        } catch (Exception e) {
            log.error("❌ ========================================");
            log.error("❌ [MANUAL] Force refresh FAILED!");
            log.error("❌ Error: {}", e.getMessage());
            log.error("❌ ========================================");
            throw new RuntimeException("Failed to refresh cache: " + e.getMessage(), e);
        }
    }

    public void manualRefreshSection(String sectionKey) {
        log.warn("🚨 [MANUAL] Refreshing section: {}", sectionKey);

        switch (sectionKey.toLowerCase().trim()) {
            case "raw" -> {
                clearSectionCache("raw");
                cachedSectionService.fetchHomepageRaw();
            }
            case "section1" -> {
                clearSectionsCache(List.of("raw", "section1"));
                cachedSectionService.fetchSection1();
            }

            case "section2" -> {
                clearSectionCache("section2");
                cachedSectionService.fetchSection2();
            }
            case "section3" -> {
                clearSectionCache("section3");
                cachedSectionService.fetchSection3();
            }
            case "section4" -> {
                clearSectionCache("section4");
                cachedSectionService.fetchSection4();
            }
            case "section5" -> {
                clearSectionCache("section5");
                cachedSectionService.fetchSection5();
            }
            case "section6" -> {
                clearSectionsCache(List.of("raw", "section6"));
                cachedSectionService.fetchSection6();
            }
            case "section7" -> {
                clearSectionCache("section7");
                cachedSectionService.fetchSection7();
            }
            case "section8" -> {
                clearSectionCache("section8");
                cachedSectionService.fetchSection8();
            }
            case "section9" -> {
                clearSectionCache("section9");
                cachedSectionService.fetchSection9();
            }
            case "section10" -> {
                clearSectionCache("section10");
                cachedSectionService.fetchSection10();
            }
            case "section11" -> {
                clearSectionCache("section11");
                cachedSectionService.fetchSection11();
            }
            case "section12" -> {
                clearSectionCache("section12");
                cachedSectionService.fetchSection12();
            }
            default -> throw new IllegalArgumentException("Unknown section: " + sectionKey);
        }

    }

    public void manualRefreshGroup(String group) {
        log.warn("🚨 [MANUAL] Refreshing group: {}", group);

        switch (group.toLowerCase().trim()) {
            case "initial" -> {
                clearSectionsCache(List.of("raw", "section1", "section2", "section3", "section4"));
                cachedSectionService.fetchSection1();
                cachedSectionService.fetchSection2();
                cachedSectionService.fetchSection3();
                cachedSectionService.fetchSection4();
            }
            case "group1" -> {
                clearSectionsCache(List.of("section5", "section6", "section7", "section8"));
                cachedSectionService.fetchSection5();
                cachedSectionService.fetchSection6();
                cachedSectionService.fetchSection7();
                cachedSectionService.fetchSection8();
            }
            case "group2" -> {
                clearSectionsCache(List.of("section9", "section10", "section11", "section12"));
                cachedSectionService.fetchSection9();
                cachedSectionService.fetchSection10();
                cachedSectionService.fetchSection11();
                cachedSectionService.fetchSection12();
            }
            default -> throw new IllegalArgumentException("Invalid group: " + group);
        }
    }

    public void manualClearCacheByGroup(String group) {
        log.warn("🚨 [MANUAL] Clear cache group: {}", group);
        switch (group.toLowerCase()) {
            case "initial" -> clearSectionsCache(List.of("raw", "section1", "section2", "section3", "section4"));

            case "group1" -> clearSectionsCache(List.of("section5", "section6", "section7", "section8"));

            case "group2" -> clearSectionsCache(List.of("section9", "section10", "section11", "section12"));
        }
    }

    //    HELPER: CLEAR CACHE
//    @CacheEvict(value = "homepage", allEntries = true)
    public void clearAllCache() {
        log.info("🗑️ Clearing cache via @CacheEvict + Redis direct delete");
        Set<String> keys = redisTemplate.keys("homepage::*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    //    CLEAR CACHE ONE SECTION
//    @CacheEvict(value = "homepage", key = "#sectionKey")
    public void clearSectionCache(String sectionKey) {
        if (sectionKey == null && sectionKey.isEmpty()) return;
        String redisKey = "homepage::" + sectionKey;
        redisTemplate.delete(redisKey);
        log.info("🗑️ Cleared section '{}' → deleted: {}", sectionKey);
    }

    //    CLEAR MANY SECTION
    public void clearSectionsCache(List<String> sectionKeys) {
        if (sectionKeys == null || sectionKeys.isEmpty()) return;
        List<String> redisKeys = sectionKeys.stream().map(key -> key).toList();

        redisKeys.forEach(this::clearSectionCache);
        log.info("🗑️ Cleared {} sections: {} (deleted: {})", redisKeys.size(), redisKeys);
    }
}
