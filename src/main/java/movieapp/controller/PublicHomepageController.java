package movieapp.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.dto.HomepageReponse.HomepageGroupResponse;
import movieapp.dto.HomepageReponse.HomepageResponse;
import movieapp.service.CacheStrategy;
import movieapp.service.HomepageService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@Slf4j
@RequiredArgsConstructor
public class PublicHomepageController {
    private final HomepageService homepageService;
    private final CacheStrategy cacheStrategy;
    private final RedisTemplate<String, Object> redisTemplate;


    @GetMapping(value = {"/homepage", "/homepage/{group}"})
    public HomepageGroupResponse getHomepageGrouped(
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

        return response;
    }

    @GetMapping("/fullHomepage")
    public ResponseEntity<?> getHomepage() {
        log.info("📥 Received request for FULL homepage data");
        HomepageResponse response = homepageService.getHomepageData();
        return ResponseEntity.ok(response);
    }
}
