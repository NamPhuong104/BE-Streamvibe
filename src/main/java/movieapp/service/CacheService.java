package movieapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.dto.Cache.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CacheService {

    private static final String CACHE_PREFIX = "homepage::";
    // Section to Group mapping
    private static final Map<String, String> SECTION_GROUP_MAP = Map.ofEntries(
            Map.entry("raw", "initial"),
            Map.entry("section1", "initial"),
            Map.entry("section2", "initial"),
            Map.entry("section3", "initial"),
            Map.entry("section4", "initial"),
            Map.entry("section5", "group1"),
            Map.entry("section6", "group1"),
            Map.entry("section7", "group1"),
            Map.entry("section8", "group1"),
            Map.entry("section9", "group2"),
            Map.entry("section10", "group2"),
            Map.entry("section11", "group2"),
            Map.entry("section12", "group2")
    );
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheStrategy cacheStrategy;
    private final ObjectMapper objectMapper;

    // ==================== LIST CACHE ====================

    public CacheListResponse listCache(
            Integer page,
            Integer size,
            String group,
            String status,
            String search
    ) {
        log.info("📊 Listing cache - page: {}, size: {}, group: {}, status: {}, search: {}",
                page, size, group, status, search);

        Set<String> allKeys = redisTemplate.keys(CACHE_PREFIX + "*");
        if (allKeys == null || allKeys.isEmpty()) {
            return buildEmptyListResponse(page, size, group, status, search);
        }

        // Convert to CacheItemDTO
        List<CacheItemDTO> allItems = allKeys.stream()
                .map(this::buildCacheItemDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Apply filters
        List<CacheItemDTO> filteredItems = applyFilters(allItems, group, status, search);

        // Sort by section number
        filteredItems.sort(Comparator.comparing(item -> {
            String section = item.getSection();
            if (section == null || section.equals("raw")) return 0;
            try {
                return Integer.parseInt(section.replace("section", ""));
            } catch (NumberFormatException e) {
                return 999;
            }
        }));

        // Pagination
        int totalItems = filteredItems.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, totalItems);

        List<CacheItemDTO> pagedItems = startIndex < totalItems
                ? filteredItems.subList(startIndex, endIndex)
                : Collections.emptyList();

        return CacheListResponse.builder()
                .items(pagedItems)
                .meta(CacheListResponse.PageMeta.builder()
                        .page(page)
                        .size(size)
                        .totalItems(totalItems)
                        .totalPages(totalPages)
                        .hasNext(page < totalPages)
                        .hasPrevious(page > 1)
                        .build())
                .filter(CacheListResponse.FilterInfo.builder()
                        .group(group)
                        .status(status)
                        .search(search)
                        .build())
                .build();
    }

    // ==================== CACHE STATS ====================

    public CacheStatsDTO getStats() {
        log.info("📊 Getting cache stats...");

        Set<String> allKeys = redisTemplate.keys(CACHE_PREFIX + "*");
        if (allKeys == null) allKeys = Collections.emptySet();

        List<CacheItemDTO> allItems = allKeys.stream()
                .map(this::buildCacheItemDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int activeKeys = (int) allItems.stream()
                .filter(item -> "CACHED".equals(item.getStatus()))
                .count();

        int expiredKeys = (int) allItems.stream()
                .filter(item -> "EXPIRED".equals(item.getStatus()))
                .count();

        long totalSize = allItems.stream()
                .mapToLong(item -> item.getSizeBytes() != null ? item.getSizeBytes() : 0)
                .sum();

        // Group stats
        Map<String, CacheStatsDTO.GroupStats> groupStats = new LinkedHashMap<>();
        for (String group : Arrays.asList("initial", "group1", "group2")) {
            List<CacheItemDTO> groupItems = allItems.stream()
                    .filter(item -> group.equals(item.getGroup()))
                    .collect(Collectors.toList());

            int cached = (int) groupItems.stream()
                    .filter(item -> "CACHED".equals(item.getStatus()))
                    .count();

            int expired = (int) groupItems.stream()
                    .filter(item -> "EXPIRED".equals(item.getStatus()))
                    .count();

            long avgTtl = (long) groupItems.stream()
                    .filter(item -> item.getTtlSeconds() != null && item.getTtlSeconds() > 0)
                    .mapToLong(CacheItemDTO::getTtlSeconds)
                    .average()
                    .orElse(0);

            groupStats.put(group, CacheStatsDTO.GroupStats.builder()
                    .group(group)
                    .totalSections(groupItems.size())
                    .cachedSections(cached)
                    .expiredSections(expired)
                    .avgTtlSeconds(avgTtl)
                    .build());
        }

        return CacheStatsDTO.builder()
                .totalKeys(allKeys.size())
                .activeKeys(activeKeys)
                .expiredKeys(expiredKeys)
                .totalSizeBytes(totalSize)
                .totalSizeFormatted(formatBytes(totalSize))
                .groupStats(groupStats)
                .config(CacheStatsDTO.CacheConfig.builder()
                        .ttlMinutes(60)
                        .warmUpDelaySeconds(10)
                        .refreshSchedule("minute_54: initial, minute_55: group1, minute_56: group2")
                        .build())
                .build();
    }

    // ==================== CACHE DETAIL ====================

    public CacheDetailResponse getCacheDetail(String section) {
        log.info("📊 Getting cache detail for: {}", section);

        String key = CACHE_PREFIX + section;

        if (!redisTemplate.hasKey(key)) {
            return CacheDetailResponse.builder()
                    .build();
        }

        CacheItemDTO info = buildCacheItemDTO(key);
        Object data = redisTemplate.opsForValue().get(key);

        String preview = null;
        try {
            String json = objectMapper.writeValueAsString(data);
            preview = json.length() > 1000 ? json.substring(0, 1000) + "..." : json;
        } catch (JsonProcessingException e) {
            preview = "Unable to serialize data";
        }

        return CacheDetailResponse.builder()
                .info(info)
                .data(data)
                .dataPreview(preview)
                .build();
    }

    // ==================== REFRESH ACTIONS ====================

    public CacheActionResponse refreshSection(String section) {
        log.info("🔄 Refreshing section: {}", section);
        long startTime = System.currentTimeMillis();

        try {
            cacheStrategy.manualRefreshSection(section);
            long duration = System.currentTimeMillis() - startTime;

            return CacheActionResponse.builder()
                    .action("REFRESH")
                    .target(section)
                    .durationMs(duration)
                    .affectedCount(1)
                    .affectedKeys(List.of(CACHE_PREFIX + section))
                    .build();

        } catch (Exception e) {
            log.error("❌ Failed to refresh section {}: {}", section, e.getMessage());
            return CacheActionResponse.builder()
                    .action("REFRESH")
                    .target(section)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .errors(List.of(CacheActionResponse.ErrorDetail.builder()
                            .key(section)
                            .error(e.getMessage())
                            .build()))
                    .build();
        }
    }

    public CacheActionResponse refreshGroup(String group) {
        log.info("🔄 Refreshing group: {}", group);
        long startTime = System.currentTimeMillis();

        List<String> sectionsInGroup = SECTION_GROUP_MAP.entrySet().stream()
                .filter(entry -> group.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<String> affectedKeys = new ArrayList<>();
        List<CacheActionResponse.ErrorDetail> errors = new ArrayList<>();

        try {
            cacheStrategy.manualRefreshGroup(group);

            for (String section : sectionsInGroup) {
                affectedKeys.add(CACHE_PREFIX + section);
            }

            long duration = System.currentTimeMillis() - startTime;

            return CacheActionResponse.builder()
                    .action("REFRESH")
                    .target(group)
                    .durationMs(duration)
                    .affectedCount(affectedKeys.size())
                    .affectedKeys(affectedKeys)
                    .build();

        } catch (Exception e) {
            log.error("❌ Failed to refresh group {}: {}", group, e.getMessage());
            return CacheActionResponse.builder()
                    .action("REFRESH")
                    .target(group)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .errors(List.of(CacheActionResponse.ErrorDetail.builder()
                            .key(group)
                            .error(e.getMessage())
                            .build()))
                    .build();
        }
    }

    public CacheActionResponse refreshAll() {
        log.info("🔄 Refreshing all cache...");
        long startTime = System.currentTimeMillis();

        try {
            cacheStrategy.manualRefreshAll();

            Set<String> allKeys = redisTemplate.keys(CACHE_PREFIX + "*");
            List<String> keyList = allKeys != null ? new ArrayList<>(allKeys) : Collections.emptyList();

            long duration = System.currentTimeMillis() - startTime;

            return CacheActionResponse.builder()
                    .action("REFRESH")
                    .target("all")
                    .durationMs(duration)
                    .affectedCount(keyList.size())
                    .affectedKeys(keyList)
                    .build();

        } catch (Exception e) {
            log.error("❌ Failed to refresh all: {}", e.getMessage());
            return CacheActionResponse.builder()
                    .action("REFRESH")
                    .target("all")
                    .durationMs(System.currentTimeMillis() - startTime)
                    .errors(List.of(CacheActionResponse.ErrorDetail.builder()
                            .key("all")
                            .error(e.getMessage())
                            .build()))
                    .build();
        }
    }

    // ==================== CLEAR ACTIONS ====================

    public CacheActionResponse clearSection(String section) {
        log.info("🗑️ Clearing section: {}", section);
        long startTime = System.currentTimeMillis();

        String key = CACHE_PREFIX + section;

        try {
            Boolean deleted = redisTemplate.delete(key);
            long duration = System.currentTimeMillis() - startTime;

            return CacheActionResponse.builder()
                    .action("CLEAR")
                    .target(section)
                    .durationMs(duration)
                    .affectedCount(deleted ? 1 : 0)
                    .affectedKeys(deleted ? List.of(key) : Collections.emptyList())
                    .build();

        } catch (Exception e) {
            log.error("❌ Failed to clear section {}: {}", section, e.getMessage());
            return CacheActionResponse.builder()
                    .action("CLEAR")
                    .target(section)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .errors(List.of(CacheActionResponse.ErrorDetail.builder()
                            .key(section)
                            .error(e.getMessage())
                            .build()))
                    .build();
        }
    }

    public CacheActionResponse clearGroup(String group) {
        log.info("🗑️ Clearing group: {}", group);
        long startTime = System.currentTimeMillis();

        List<String> sectionsInGroup = SECTION_GROUP_MAP.entrySet().stream()
                .filter(entry -> group.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<String> keysToDelete = sectionsInGroup.stream()
                .map(section -> CACHE_PREFIX + section)
                .collect(Collectors.toList());

        try {
            Long deletedCount = redisTemplate.delete(keysToDelete);
            long duration = System.currentTimeMillis() - startTime;

            return CacheActionResponse.builder()
                    .action("CLEAR")
                    .target(group)
                    .durationMs(duration)
                    .affectedCount(deletedCount != null ? deletedCount.intValue() : 0)
                    .affectedKeys(keysToDelete)
                    .build();

        } catch (Exception e) {
            log.error("❌ Failed to clear group {}: {}", group, e.getMessage());
            return CacheActionResponse.builder()
                    .action("CLEAR")
                    .target(group)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .errors(List.of(CacheActionResponse.ErrorDetail.builder()
                            .key(group)
                            .error(e.getMessage())
                            .build()))
                    .build();
        }
    }

    public CacheActionResponse clearAll() {
        log.info("🗑️ Clearing all cache...");
        long startTime = System.currentTimeMillis();

        try {
            Set<String> allKeys = redisTemplate.keys(CACHE_PREFIX + "*");
            List<String> keyList = allKeys != null ? new ArrayList<>(allKeys) : Collections.emptyList();

            if (!keyList.isEmpty()) {
                redisTemplate.delete(keyList);
            }

            long duration = System.currentTimeMillis() - startTime;

            return CacheActionResponse.builder()
                    .action("CLEAR")
                    .target("all")
                    .durationMs(duration)
                    .affectedCount(keyList.size())
                    .affectedKeys(keyList)
                    .build();

        } catch (Exception e) {
            log.error("❌ Failed to clear all: {}", e.getMessage());
            return CacheActionResponse.builder()
                    .action("CLEAR")
                    .target("all")
                    .durationMs(System.currentTimeMillis() - startTime)
                    .errors(List.of(CacheActionResponse.ErrorDetail.builder()
                            .key("all")
                            .error(e.getMessage())
                            .build()))
                    .build();
        }
    }

    // ==================== HELPER METHODS ====================

    private CacheItemDTO buildCacheItemDTO(String fullKey) {
        try {
            String section = fullKey.replace(CACHE_PREFIX, "");
            String group = SECTION_GROUP_MAP.getOrDefault(section, "unknown");

            Long ttlSeconds = redisTemplate.getExpire(fullKey, TimeUnit.SECONDS);
            boolean isCached = ttlSeconds != null && ttlSeconds > 0;

            // Get size
            Object value = redisTemplate.opsForValue().get(fullKey);
            long sizeBytes = 0;
            int itemCount = 0;
            String type = "UNKNOWN";

            if (value != null) {
                try {
                    String json = objectMapper.writeValueAsString(value);
                    sizeBytes = json.getBytes().length;

                    if (value instanceof List) {
                        type = "LIST";
                        itemCount = ((List<?>) value).size();
                    } else if (value instanceof Map) {
                        type = "OBJECT";
                    } else if (value instanceof String) {
                        type = "STRING";
                    }
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize cache value for size calculation");
                }
            }

            return CacheItemDTO.builder()
                    .key(fullKey)
                    .section(section)
                    .group(group)
                    .status(isCached ? "CACHED" : "EXPIRED")
                    .ttlSeconds(isCached ? ttlSeconds : 0L)
                    .ttlMinutes(isCached ? ttlSeconds / 60 : 0L)
                    .expiresAt(isCached ? new Date(System.currentTimeMillis() + (ttlSeconds * 1000)) : null)
                    .sizeBytes(sizeBytes)
                    .sizeFormatted(formatBytes(sizeBytes))
                    .itemCount(itemCount)
                    .type(type)
                    .build();

        } catch (Exception e) {
            log.error("Error building CacheItemDTO for key {}: {}", fullKey, e.getMessage());
            return null;
        }
    }

    private List<CacheItemDTO> applyFilters(
            List<CacheItemDTO> items,
            String group,
            String status,
            String search
    ) {
        return items.stream()
                .filter(item -> {
                    // Filter by group
                    if (group != null && !group.isEmpty() && !group.equals(item.getGroup())) {
                        return false;
                    }
                    // Filter by status
                    if (status != null && !status.isEmpty() && !status.equalsIgnoreCase(item.getStatus())) {
                        return false;
                    }
                    // Filter by search
                    if (search != null && !search.isEmpty()) {
                        String searchLower = search.toLowerCase();
                        return item.getKey().toLowerCase().contains(searchLower) ||
                                item.getSection().toLowerCase().contains(searchLower);
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private CacheListResponse buildEmptyListResponse(
            Integer page, Integer size, String group, String status, String search
    ) {
        return CacheListResponse.builder()
                .items(Collections.emptyList())
                .meta(CacheListResponse.PageMeta.builder()
                        .page(page)
                        .size(size)
                        .totalItems(0)
                        .totalPages(0)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build())
                .filter(CacheListResponse.FilterInfo.builder()
                        .group(group)
                        .status(status)
                        .search(search)
                        .build())
                .build();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}