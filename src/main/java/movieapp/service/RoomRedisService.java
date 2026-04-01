package movieapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.dto.Room.Chat.ChatMessageDTO;
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
    private static final String ROOM_CHAT_KEY = "room:%s:chat";
    private static final String CHAT_RATE_KEY = "chat:rate:%s:%s";
    private static final Duration CHAT_RATE_LIMIT = Duration.ofSeconds(2);
    private static final String ROOM_HEARTBEAT_KEY = "room:%s:heartbeat";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // ==================== HEARTBEAT ====================
    public void updateHeartbeat(String code, Long userId) {
        String key = String.format(ROOM_HEARTBEAT_KEY, code);
        redisTemplate.opsForHash().put(key, userId.toString(), String.valueOf(System.currentTimeMillis()));

        redisTemplate.expire(key, ROOM_TTL);
    }

    public Long getLastHeartbeat(String code, Long userId) {
        String key = String.format(ROOM_HEARTBEAT_KEY, code);
        Object val = redisTemplate.opsForHash().get(key, userId.toString());
        if (val == null) return null;

        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void removeHeartbeat(String code, Long userId) {
        String key = String.format(ROOM_HEARTBEAT_KEY, code);
        redisTemplate.opsForHash().delete(key, userId.toString());
    }


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
                String.format(ROOM_SUGGESTS_KEY, code),
                String.format(ROOM_CHAT_KEY, code),
                String.format(ROOM_HEARTBEAT_KEY, code)
        ));

        // Xóa rate limit keys cho room này
        //  TIP: Redis KEYS command dùng pattern matching
        //    Nhưng KEYS chậm trên production (scan toàn bộ keyspace)
        //    → Ở đây OK vì rate limit keys tự hết hạn sau 2s
        //    → Không cần xóa thủ công

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

    public long getPendingCount(String code) {
        String key = String.format(ROOM_PENDING_KEY, code);
        Long count = redisTemplate.opsForSet().size(key);

        return count != null ? count : 0;
    }

    // ==================== MOVIE SUGGESTIONS ====================
    public List<MovieSuggestDTO> getSuggestions(String code) {
        String key = String.format(ROOM_SUGGESTS_KEY, code);
        List<String> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null) return List.of();

        return raw.stream().map(json -> {
            try {
                return objectMapper.readValue(json, MovieSuggestDTO.class);
            } catch (Exception e) {
                log.error("Error deserializing suggestion", e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public boolean suggestionExists(String code, String movieSlug) {
        String key = String.format(ROOM_SUGGESTS_KEY, code);
        List<String> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null) return false;

        return raw.stream().anyMatch(json -> {
                    try {
                        MovieSuggestDTO dto = objectMapper.readValue(json, MovieSuggestDTO.class);
                        return movieSlug.equals(dto.getMovieSlug());
                    } catch (Exception e) {
                        return false;
                    }
                }
        );
    }


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


    public void removeSuggestionBySlug(String code, String movieSlug) {
        String key = String.format(ROOM_SUGGESTS_KEY, code);
        List<String> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null) return;

        for (String json : raw) {
            try {
                MovieSuggestDTO dto = objectMapper.readValue(json, MovieSuggestDTO.class);
                if (movieSlug.equals(dto.getMovieSlug())) {
                    redisTemplate.opsForList().remove(key, 1, json);
                }
            } catch (Exception e) {
                log.error("Error deserializing suggestion for removal", e);
            }
        }
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

// ==================== CHAT MESSAGES ====================

    /**
     * Thêm tin nhắn vào cuối list
     * <p>
     * Redis command: RPUSH room:{code}:chat "{json}"
     * <p>
     * RPUSH = Right Push = thêm vào cuối list
     * → Tin nhắn cũ ở đầu, mới ở cuối = chronological order
     */
    public void addChatMessage(String code, ChatMessageDTO message) {
        try {
            String key = String.format(ROOM_CHAT_KEY, code);
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().rightPush(key, json);
            redisTemplate.expire(key, ROOM_TTL);
        } catch (Exception e) {
            log.error("Error saving chat message for room {}", code, e);
        }
    }

    /**
     * Lấy toàn bộ lịch sử chat
     * <p>
     * Redis command: LRANGE room:{code}:chat 0 -1
     * <p>
     * LRANGE key start stop
     * - 0 = phần tử đầu tiên
     * - -1 = phần tử cuối cùng
     * → LRANGE 0 -1 = lấy TẤT CẢ
     */
    public List<ChatMessageDTO> getChatHistory(String code) {
        String key = String.format(ROOM_CHAT_KEY, code);
        List<String> raw = redisTemplate.opsForList().range(key, 0, -1);

        if (raw == null) return List.of();

        return raw.stream().map(json -> {
            try {
                return objectMapper.readValue(json, ChatMessageDTO.class);
            } catch (Exception e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Tìm tin nhắn theo ID
     * <p>
     * Redis List không hỗ trợ lookup by field (khác Hash)
     * → Phải iterate qua tất cả elements
     * → OK vì chat room thường < 1000 messages
     */
    public ChatMessageDTO findChatMessageById(String code, String messageId) {
        String key = String.format(ROOM_CHAT_KEY, code);
        List<String> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null) return null;

        for (String json : raw) {
            try {
                ChatMessageDTO msg = objectMapper.readValue(json, ChatMessageDTO.class);
                if (messageId.equals(msg.getId())) return msg;
            } catch (Exception e) {
                //
            }
        }

        return null;
    }

    /**
     * Soft delete tin nhắn (đánh dấu deleted, xóa content)
     * <p>
     * Flow:
     * 1. LRANGE lấy tất cả messages
     * 2. Tìm message có matching ID
     * 3. LSET tại index đó với updated JSON (deleted=true, content="")
     * <p>
     * LSET key index value
     * - Thay thế element tại vị trí index
     * - O(1) cho head/tail, O(n) cho middle (nhưng n nhỏ)
     * <p>
     * Tại sao Soft Delete thay vì Hard Delete (LREM)?
     * - Giữ nguyên thứ tự và index của các messages khác
     * - Client hiển thị "Tin nhắn đã bị xóa" thay vì biến mất
     * - Các reply tới message này vẫn hiển thị đúng
     */
    public boolean softDeleteChatMessage(String code, String messageId) {
        String key = String.format(ROOM_CHAT_KEY, code);
        List<String> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null) return false;

        for (int i = 0; i < raw.size(); i++) {
            try {
                ChatMessageDTO msg = objectMapper.readValue(raw.get(i), ChatMessageDTO.class);
                if (messageId.equals(msg.getId())) {
                    msg.setDeleted(true);
                    msg.setContent("");
                    String updatedJson = objectMapper.writeValueAsString(msg);
                    redisTemplate.opsForList().set(key, i, updatedJson);

                    return true;
                }

            } catch (Exception e) {
                log.error("Error processing message for deletion", e);
            }
        }
        return false;
    }

    /**
     * Đếm số tin nhắn trong room
     * <p>
     * Redis command: LLEN room:{code}:chat
     */
    public long getChatMessageCount(String code) {
        String key = String.format(ROOM_CHAT_KEY, code);
        Long count = redisTemplate.opsForList().size(key);

        return count != null ? count : 0;
    }

    // ==================== RATE LIMITING ====================

    /**
     * Check và set rate limit cho user
     * <p>
     * Redis command: SET chat:rate:{code}:{userId} "1" NX EX 2
     * <p>
     * Giải thích SETNX + TTL:
     * - NX (Not eXists): chỉ set nếu key CHƯA tồn tại
     * - EX 2: key tự hết hạn sau 2 giây
     * <p>
     * Flow:
     * - User gửi tin nhắn lần 1 → key chưa tồn tại → SETNX trả TRUE → cho phép
     * - User gửi tin nhắn lần 2 (trong 2s) → key đã tồn tại → SETNX trả FALSE → block
     * - Sau 2 giây → key tự hết hạn → user gửi được tiếp
     * <p>
     * Ưu điểm:
     * - Atomic (thread-safe, không cần synchronized)
     * - Tự cleanup (TTL)
     * - Stateless trên server (mọi state ở Redis)
     *
     * @return TRUE nếu bị rate limited (không cho gửi)
     */
    public boolean isChatRateLimited(String code, Long userId) {
        String key = String.format(CHAT_RATE_KEY, code, userId);
        Boolean wasSet = redisTemplate.opsForValue().setIfAbsent(key, "1", CHAT_RATE_LIMIT);

        return !Boolean.TRUE.equals(wasSet);
    }

    // ==================== PENDING REQUESTS (ADMIN) ====================

    /**
     * Lấy danh sách pending requests — dùng cho admin detail view
     */
    public List<JoinRequestDTO> getPendingRequests(String code) {
        String key = String.format(ROOM_PENDING_KEY, code);
        Set<String> pending = redisTemplate.opsForSet().members(key);
        if (pending == null) return List.of();

        return pending.stream().map(json -> {
            try {
                return objectMapper.readValue(json, JoinRequestDTO.class);
            } catch (Exception e) {
                log.error("Error deserializing pending request", e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
}