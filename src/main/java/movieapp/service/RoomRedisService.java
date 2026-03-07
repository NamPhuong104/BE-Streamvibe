package movieapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.dto.Room.JoinRequestDTO;
import movieapp.dto.Room.MovieSuggestDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomRedisService {
    private static final String ROOM_KEY = "room:%s";
    private static final String ROOM_MEMBERS_KEYS = "room:%s:members";
    private static final String ROOM_PENDING_KEY = "room:%s:pending";
    private static final String ROOM_SUGGESTS_KEY = "room:%s:suggests";
    private static final String USER_ROOM_KEY = "user:room:%s";
    private static final String ROOM_LIST_KEY = "rooms:active";
    private static final Duration ROOM_TTL = Duration.ofDays(7);
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;


    // ==================== ROOM CRUD ====================

    public void createRoom(String code, Map<String, String> roomData) {
        String key = String.format(ROOM_KEY, code);
        redisTemplate.opsForHash().putAll(key, roomData);
        redisTemplate.expire(key, ROOM_TTL);

        redisTemplate.opsForSet().add(ROOM_LIST_KEY, code);
    }

    public Map<Object, Object> getRoom(String code) {
        String key = String.format(ROOM_KEY, code);

        return redisTemplate.opsForHash().entries(key);
    }

    public boolean roomExists(String code) {
        String key = String.format(ROOM_KEY, code);

        return redisTemplate.hasKey(key);
    }

    public void updateRoomField(String code, String field, String value) {
        String key = String.format(ROOM_KEY, code);
        redisTemplate.opsForHash().put(key, field, value);
    }

    public void updateRoomFields(String code, Map<String, String> fields) {
        String key = String.format(ROOM_KEY, code);
        redisTemplate.opsForHash().putAll(key, fields);
    }

    public String getRoomField(String code, String field) {
        String key = String.format(ROOM_KEY, code);
        Object value = redisTemplate.opsForHash().get(key, field);

        return value != null ? value.toString() : null;
    }

    public void deleteRoom(String code) {
        redisTemplate.delete(List.of(
                String.format(ROOM_KEY, code),
                String.format(ROOM_MEMBERS_KEYS, code),
                String.format(ROOM_PENDING_KEY, code),
                String.format(ROOM_SUGGESTS_KEY, code))
        );

        redisTemplate.opsForSet().remove(ROOM_LIST_KEY, code);
        log.info("Room {} deleted from Redis", code);
    }

    // ==================== MEMBERS ====================
    public void addMember(String code, Long userId) {
        String key = String.format(ROOM_MEMBERS_KEYS, code);
        redisTemplate.opsForSet().add(key, userId.toString());
        redisTemplate.expire(key, ROOM_TTL);

        String userKey = String.format(USER_ROOM_KEY, userId);
        redisTemplate.opsForValue().set(userKey, code, ROOM_TTL);
    }

    public void removeMember(String code, Long userId) {
        String key = String.format(ROOM_MEMBERS_KEYS, code);
        redisTemplate.opsForSet().remove(key, userId.toString());

        String userKey = String.format(USER_ROOM_KEY, userId);
        redisTemplate.delete(userKey);
    }

    public Set<String> getMembers(String code) {
        String key = String.format(ROOM_MEMBERS_KEYS, code);
        Set<String> members = redisTemplate.opsForSet().members(key);

        return members != null ? members : Set.of();
    }

    public long getMemberCount(String code) {
        String key = String.format(ROOM_MEMBERS_KEYS, code);
        Long count = redisTemplate.opsForSet().size(key);

        return count != null ? count : 0;
    }

    public boolean isMember(String code, Long userId) {
        String key = String.format(ROOM_MEMBERS_KEYS, code);

        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, userId.toString()));
    }


    // ==================== USER ↔ ROOM MAPPING ====================
    public String getUserRoom(Long userId) {
        String key = String.format(USER_ROOM_KEY, userId);

        return redisTemplate.opsForValue().get(key);
    }

    public void removeUserRoom(Long userId) {
        String key = String.format(USER_ROOM_KEY, userId);

        redisTemplate.delete(key);
    }

    // ==================== PENDING REQUESTS ====================
    public void addPendingRequest(String code, JoinRequestDTO request) {
        try {
            String key = String.format(ROOM_PENDING_KEY, code);
            String json = objectMapper.writeValueAsString(request);
            redisTemplate.opsForSet().add(key, json);
            redisTemplate.expire(key, ROOM_TTL);
        } catch (Exception e) {
            log.error("Error serializing join request", e);
        }
    }

    public void removePendingRequest(String code, Long userId) {
        String key = String.format(ROOM_PENDING_KEY, code);
        Set<String> pending = redisTemplate.opsForSet().members(key);

        if (pending == null) return;

        for (String json : pending) {
            try {
                JoinRequestDTO request = objectMapper.readValue(json, JoinRequestDTO.class);
                if (request.getUserId().equals(userId)) {
                    redisTemplate.opsForSet().remove(key, json);
                    break;
                }
            } catch (Exception e) {
                log.error("Error deserializing join request", e);
            }
        }
    }

    public List<JoinRequestDTO> getPendingRequests(String code) {
        String key = String.format(ROOM_PENDING_KEY, code);
        Set<String> pending = redisTemplate.opsForSet().members(key);

        if (pending == null) return List.of();

        return pending.stream().map(json -> {
            try {
                return objectMapper.readValue(json, JoinRequestDTO.class);
            } catch (Exception e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public long getPendingCount(String code) {
        String key = String.format(ROOM_PENDING_KEY, code);
        Long count = redisTemplate.opsForSet().size(key);

        return count != null ? count : 0;
    }

    // ==================== MOVIE SUGGESTIONS ====================
    public void addSuggestion(String code, MovieSuggestDTO suggestion) {
        try {
            String key = String.format(ROOM_SUGGESTS_KEY, code);
            String json = objectMapper.writeValueAsString(suggestion);
            redisTemplate.opsForList().rightPush(key, json);
            redisTemplate.expire(key, ROOM_TTL);
        } catch (Exception e) {
            log.error("Error serializing suggestion", e);
        }
    }

    public List<MovieSuggestDTO> getSuggestions(String code) {
        String key = String.format(ROOM_SUGGESTS_KEY, code);
        List<String> raw = redisTemplate.opsForList().range(key, 0, -1);

        if (raw == null) return List.of();

        return raw.stream().map(json -> {
            try {
                return objectMapper.readValue(json, MovieSuggestDTO.class);
            } catch (JsonProcessingException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public long getSuggestionCount(String code) {
        String key = String.format(ROOM_SUGGESTS_KEY, code);
        Long count = redisTemplate.opsForList().size(key);

        return count != null ? count : 0;
    }

    public void clearSuggestions(String code) {
        String key = String.format(ROOM_SUGGESTS_KEY, code);
        redisTemplate.delete(key);
    }

    // ==================== BROWSE ROOMS ====================
    public Set<String> getActiveRoomCodes() {
        Set<String> codes = redisTemplate.opsForSet().members(ROOM_LIST_KEY);

        return codes != null ? codes : Set.of();
    }

    // ==================== ROOM CODE GENERATION ====================
    public String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            code = generateCode();
            attempts++;

            if (attempts > 100)
                throw new RuntimeException("Cannot generate unique room code");

        } while (roomExists(code));

        return code;
    }

    private String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(6);
        Random random = new Random();
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(random.nextInt(chars.length())));

        return sb.toString();
    }
}
