package movieapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.dto.Room.Chat.ChatMessageDTO;
import movieapp.dto.Room.Chat.ChatSendDTO;
import movieapp.dto.Room.*;
import movieapp.entity.User;
import movieapp.exception.CommonMessageException;
import movieapp.repository.UserRepository;
import movieapp.util.SecurityUtil;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {
    private static final int DEFAULT_MAX_MEMBERS = 999;
    private static final long HOST_GRACE_PERIOD_SECONDS = 180; // 3 minutes
    private static final long MEMBER_GRACE_PERIOD_SECONDS = 180;

    private final RoomRedisService roomRedis;
    private final UserRepository userRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final WsUserCacheService wsUserCache;
    private final RoomChatService roomChatService;

    // Track host disconnect time for grace period
    private final Map<String, Instant> hostDisconnectTimes = new java.util.concurrent.ConcurrentHashMap<>();
    // Track member disconnect — key = "roomCode:userId"
    private final Map<String, Instant> memberDisconnectTimes = new java.util.concurrent.ConcurrentHashMap<>();

    // ==================== HEARTBEAT ====================
    public void handleHeartbeat(String code, Principal principal) {
        Long userId = wsUserCache.getUserIdFromPrincipal(principal);
        if (!roomRedis.roomExists(code) || !roomRedis.isMember(code, userId)) return;

        roomRedis.updateHeartbeat(code, userId);
    }

    // ==================== CREATE ROOM ====================
    public RoomResponse createRoom(RoomCreateDTO dto) {
        User currentUser = userService.getCurrentUser();
        String existingRoom = roomRedis.getUserRoom(currentUser.getId());
        if (existingRoom != null)
            throw new CommonMessageException("Bạn đang ở phòng " + existingRoom + ". Vui lòng rời phòng trước.");

        String code = roomRedis.generateUniqueCode();

        String roomName = dto.getRoomName() != null && !dto.getRoomName().isBlank() ? dto.getRoomName() : currentUser.getFullName();

        // Create room data
        Map<String, String> roomData = new HashMap<>();
        roomData.put("code", code);
        roomData.put("hostId", currentUser.getId().toString());
        roomData.put("hostFullName", currentUser.getFullName());
        roomData.put("hostUsername", currentUser.getUsername());
        roomData.put("hostAvatarUrl", currentUser.getAvatarUrl() != null ? currentUser.getAvatarUrl() : "");
        roomData.put("roomName", roomName);
        roomData.put("state", "WAITING");
        roomData.put("maxMembers", String.valueOf(DEFAULT_MAX_MEMBERS));
        roomData.put("requireApproval", String.valueOf(dto.isRequireApproval()));
        roomData.put("currentTime", "0");
        roomData.put("createdAt", Instant.now().toString());

        if (dto.getMovieSlug() != null && !dto.getMovieSlug().isBlank()) {
            roomData.put("movieSlug", dto.getMovieSlug());
            roomData.put("movieName", dto.getMovieName() != null ? dto.getMovieName() : "");
            roomData.put("moviePosterUrl", dto.getMoviePosterUrl() != null ? dto.getMoviePosterUrl() : "");
            roomData.put("movieThumbUrl", dto.getMovieThumbUrl() != null ? dto.getMovieThumbUrl() : "");
            roomData.put("currentEpisode", "");
            roomData.put("currentServerIndex", "0");
            roomData.put("videoUrl", "");
        }

        roomRedis.createRoom(code, roomData);

        // Add host as first member
        roomRedis.addMember(code, currentUser.getId());
        roomRedis.updateHeartbeat(code, currentUser.getId());
        log.info("Room {} created by user {} ({})", code,
                currentUser.getUsername(), currentUser.getId());

        return buildRoomResponse(code);
    }

    // ==================== JOIN ROOM ====================
    public RoomResponse joinRoom(String code) {
        User currentUser = userService.getCurrentUser();
        validateRoomExists(code);

        String existingRoom = roomRedis.getUserRoom(currentUser.getId());
        if (existingRoom != null && !existingRoom.equals(code))
            throw new CommonMessageException("Bạn đang ở phòng " + existingRoom + ". Vui lòng rời phòng trước.");

        if (roomRedis.isMember(code, currentUser.getId())) return buildRoomResponse(code);

        boolean requireApproval = Boolean.parseBoolean(roomRedis.getRoomField(code, "requireApproval"));
        boolean isAdminUser = SecurityUtil.isAdmin();

        if (requireApproval && !isAdminUser) {
            JoinRequestDTO request = JoinRequestDTO.builder()
                    .userId(currentUser.getId())
                    .fullName(currentUser.getFullName())
                    .username(currentUser.getUsername())
                    .avatarUrl(currentUser.getAvatarUrl())
                    .requestedAt(Instant.now().toEpochMilli())
                    .build();

            roomRedis.addPendingRequest(code, request);

            messagingTemplate.convertAndSend("/topic/room/" + code + "/request", request);

            throw new CommonMessageException("Yêu cầu đã được gửi. Chờ host duyệt.");
        }

        addMemberToRoom(code, currentUser);

        if (isAdminUser && requireApproval) {
            log.info("Admin {} bypassed approval for room {}", currentUser.getUsername(), code);
        }

        return buildRoomResponse(code);
    }

    // ==================== APPROVE/REJECT REQUEST ====================
    public void approveRequest(String code, Long userId) {
        validateHostOrAdmin(code);

        User user = userRepository.findById(userId).orElseThrow(() -> new CommonMessageException("User không tồn tại"));

        if (roomRedis.isMember(code, userId)) {
            roomRedis.removePendingRequest(code, userId);

            messagingTemplate.convertAndSend("/topic/room/" + code + "/approved", Map.of("userId", userId, "approved", true));
            return;
        }

        roomRedis.removePendingRequest(code, userId);
        addMemberToRoom(code, user);

        messagingTemplate.convertAndSend("/topic/room/" + code + "/approved", Map.of("userId", userId, "approved", true));
    }

    public void rejectRequest(String code, Long userId) {
        validateHostOrAdmin(code);
        roomRedis.removePendingRequest(code, userId);

        messagingTemplate.convertAndSend(
                "/topic/room/" + code + "/approved",
                Map.of("userId", userId, "approved", false)
        );
    }

    // ==================== LEAVE ROOM ====================
    public void leaveRoom(String code) {
        User currentUser = userService.getCurrentUser();
        validateRoomExists(code);

        String hostId = roomRedis.getRoomField(code, "hostId");

        if (currentUser.getId().toString().equals(hostId)) {
            closeRoom(code, "Host đã rời phòng");
        } else {
            roomRedis.removeMember(code, currentUser.getId());
            roomRedis.removeHeartbeat(code, currentUser.getId());
            memberDisconnectTimes.remove((memberKey(code, currentUser.getId())));

            String displayName = getDisplayName(currentUser);
            roomChatService.sendSystemMessage(code, displayName + " đã rời phòng");

            broadcastMembers(code);
            log.info("User {} left room {}", currentUser.getUsername(), code);
        }
    }

    // ==================== KICK USER ====================
    public void kickUser(String code, Long userId) {
        validateHostOrAdmin(code);

        String hostId = roomRedis.getRoomField(code, "hostId");
        if (userId.toString().equals(hostId)) throw new CommonMessageException("Host không thể tự kick chính mình");

        performKick(code, userId, false);
    }

    // ==================== VIDEO SYNC ====================
    public void syncState(String code, RoomStateDTO state, Principal principal) {
        Long userId = wsUserCache.getUserIdFromPrincipal(principal);
        boolean isAdmin = wsUserCache.isAdmin(principal);

        if (!isAdmin) validateHostById(code, userId);

        Map<String, String> updates = new HashMap<>();
        updates.put("currentTime", String.valueOf(state.getCurrentTime()));

        if (state.getVideoUrl() != null) updates.put("videoUrl", state.getVideoUrl());

        if (state.getEpisodeSlug() != null) updates.put("currentEpisode", state.getEpisodeSlug());
        if (state.getSpeed() > 0) updates.put("speed", String.valueOf(state.getSpeed()));

        String action = state.getAction();

        if ("PLAY".equals(action)) {
            updates.put("state", "PLAYING");
        } else if ("PAUSE".equals(action)) {
            updates.put("state", "WAITING");
        }

        updates.put("serverIndex", String.valueOf(state.getServerIndex()));
        roomRedis.updateRoomFields(code, updates);

        state.setTimestamp(System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/room/" + code + "/state", state);
    }

    // ==================== MOVIE MANAGEMENT ====================
    public void setMovie(String code, RoomResponse.RoomMovieDTO movie) {
        validateHostOrAdmin(code);

        String oldMovieSlug = roomRedis.getRoomField(code, "movieSlug");
        String oldEpisode = roomRedis.getRoomField(code, "currentEpisode");

        Map<String, String> updates = new HashMap<>();

        updates.put("movieSlug", movie.getSlug());
        updates.put("movieName", movie.getName());
        updates.put("moviePosterUrl", movie.getPosterUrl() != null ? movie.getPosterUrl() : "");
        updates.put("movieThumb", movie.getThumbUrl() != null ? movie.getThumbUrl() : "");
        updates.put("currentEpisode", movie.getCurrentEpisode() != null ? movie.getCurrentEpisode() : "1");
        updates.put("currentServerIndex", String.valueOf(movie.getCurrentServerIndex()));
        updates.put("videoUrl", movie.getVideoUrl() != null ? movie.getVideoUrl() : "");
        updates.put("currentTime", "0");
        updates.put("state", "WAITING");

        roomRedis.updateRoomFields(code, updates);

        roomRedis.removeSuggestionBySlug(code, movie.getSlug());

        if (!movie.getSlug().equals(oldMovieSlug)) {
            roomChatService.sendSystemMessage(code, "Chủ phòng đã đổi phim: " + movie.getName());
        } else if (movie.getCurrentEpisode() != null && !movie.getCurrentEpisode().equals(oldEpisode)) {
            roomChatService.sendSystemMessage(code, "Chủ phòng đã chuyển sang tập " + movie.getCurrentEpisode());
        }

        messagingTemplate.convertAndSend("/topic/room/" + code + "/movie", movie);
    }

    public void suggestMovie(String code, MovieSuggestDTO suggestion, Principal principal) {
        User currentUser = wsUserCache.getFullUser(principal);
        validateRoomExists(code);

        if (!roomRedis.isMember(code, currentUser.getId()))
            throw new CommonMessageException("Bạn không ở trong phòng này");

        String currentMovieSlug = roomRedis.getRoomField(code, "movieSlug");
        if (suggestion.getMovieSlug().equals(currentMovieSlug)) return;

        if (roomRedis.suggestionExists(code, suggestion.getMovieSlug())) return;

        suggestion.setSuggestedByUserId(currentUser.getId());
        suggestion.setSuggestedByFullName(currentUser.getFullName());
        suggestion.setSuggestedByUsername(currentUser.getUsername());
        suggestion.setSuggestedAt(Instant.now().toEpochMilli());

        roomRedis.addSuggestion(code, suggestion);

        messagingTemplate.convertAndSend("/topic/room/" + code + "/suggest", suggestion);
    }

    // ==================== ROOM SETTINGS ====================
    public void updateSettings(String code, Map<String, Object> settings) {
        validateHostOrAdmin(code);

        Map<String, String> updates = new HashMap<>();

        if (settings.containsKey("requireApproval"))
            updates.put("requireApproval", settings.get("requireApproval").toString());

        if (settings.containsKey("maxMembers")) updates.put("maxMembers", settings.get("maxMembers").toString());

        if (settings.containsKey("roomName")) updates.put("roomName", settings.get("roomName").toString());

        if (!updates.isEmpty()) roomRedis.updateRoomFields(code, updates);

        Map<String, Object> boaradcast = new HashMap<>();
        boaradcast.put("roomName", roomRedis.getRoomField(code, "roomName"));
        boaradcast.put("requireApproval", Boolean.parseBoolean(roomRedis.getRoomField(code, "requireApproval")));
        boaradcast.put("maxMembers", Integer.parseInt(roomRedis.getRoomField(code, "maxMembers")));

        messagingTemplate.convertAndSend("/topic/room/" + code + "/settings", boaradcast);
    }

    // ==================== BROWSE ROOMS ====================
    public List<RoomBrowseResponse> getActiveRooms() {
        Set<String> codes = roomRedis.getActiveRoomCodes();
        return codes.stream().map(code -> {
                    try {
                        Map<Object, Object> data = roomRedis.getRoom(code);
                        if (data.isEmpty()) {
                            roomRedis.deleteRoom(code);

                            return null;
                        }

                        return RoomBrowseResponse.builder()
                                .code(code)
                                .roomName(getStr(data, "roomName"))
                                .hostUsername(getStr(data, "hostUsername"))
                                .hostAvatarUrl(getStr(data, "hostAvatarUrl"))
                                .movieName(getStr(data, "movieName"))
                                .moviePosterUrl(getStr(data, "moviePosterUrl"))
                                .state(getStr(data, "state"))
                                .memberCount((int) roomRedis.getMemberCount(code))
                                .maxMembers(Integer.parseInt(getStr(data, "maxMembers", "10")))
                                .requireApproval(Boolean.parseBoolean(getStr(data, "requireApproval", "true")))
                                .build();
                    } catch (Exception e) {
                        log.error("Error building room browse for {}", code, e);

                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> b.getMemberCount() - a.getMemberCount())
                .collect(Collectors.toList());
    }

    public RoomResponse getRoomDetails(String code) {
        validateRoomExists(code);

        return buildRoomResponse(code);
    }

    // ==================== DISCONNECT HANDLING ====================
    public void handleUserDisconnect(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return;

        String roomCode = roomRedis.getUserRoom(user.getId());
        if (roomCode == null) return;

        if (!roomRedis.roomExists(roomCode)) {
            roomRedis.removeUserRoom(user.getId());

            return;
        }

        String hostId = roomRedis.getRoomField(roomCode, "hostId");

        if (user.getId().toString().equals(hostId)) {
            // Host disconnected → start grace period
            hostDisconnectTimes.put(roomCode, Instant.now());

            // AUTO-PAUSE: Đọc currentTime từ Redis và broadcast PAUSE
            String currentTimerStr = roomRedis.getRoomField(roomCode, "currentTime");
            double currentTime = parseDoubleSafe(currentTimerStr, 0);

            String speedStr = roomRedis.getRoomField(roomCode, "speed");
            double speed = parseDoubleSafe(speedStr, 1);

            // Update room state to WAITING (paused)
            roomRedis.updateRoomField(roomCode, "state", "WAITING");

            // Broadcast PAUSE to all members
            RoomStateDTO pauseState = RoomStateDTO.builder()
                    .action("PAUSE")
                    .currentTime(currentTime)
                    .videoUrl(null)
                    .episodeSlug(null)
                    .serverIndex(0)
                    .speed(speed)
                    .timestamp(System.currentTimeMillis())
                    .build();
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/state", pauseState);

            // Broadcast host status
            log.info("Host {} disconnected from room {}. Grace period started. currentTime={}",
                    user.getUsername(), roomCode, currentTime);
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/host-status", Map.of("status", "disconnected", "graceSeconds", HOST_GRACE_PERIOD_SECONDS, "currentTime", currentTime));

        } else {
            // ═══ MEMBER DISCONNECT → Grace period 3 phút ═══
            String key = memberKey(roomCode, user.getId());
            memberDisconnectTimes.put(key, Instant.now());

            log.info("Member {} disconnected from room {}. Grace period: {}s",
                    user.getUsername(), roomCode, MEMBER_GRACE_PERIOD_SECONDS);
        }
    }

    public void handleUserReconnect(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return;

        String roomCode = roomRedis.getUserRoom(user.getId());
        if (roomCode == null) return;

        if (!roomRedis.roomExists(roomCode)) {
            roomRedis.removeUserRoom(user.getId());
            return;
        }

        roomRedis.updateHeartbeat(roomCode, user.getId());

        String hostId = roomRedis.getRoomField(roomCode, "hostId");
        if (user.getId().toString().equals(hostId)) {
            if (hostDisconnectTimes.remove(roomCode) != null) {
                messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/host-status",
                        Map.of("status", "connected"));
                broadcastMembers(roomCode);
                log.info("Host {} reconnected to room {}. Grace period cancelled.",
                        user.getUsername(), roomCode);
            }
        } else {
            String key = memberKey(roomCode, user.getId());
            if (memberDisconnectTimes.remove(key) != null) {
                broadcastMembers(roomCode);
                log.info("Member {} reconnected to room {}. Grace period cancelled.",
                        user.getUsername(), roomCode);
            }
        }
    }

    // ==================== SCHEDULED CLEANUP ====================
    // Dọn phòng + member tự động mỗi 10 giây
    @Scheduled(fixedRate = 15_000)
    public void cleanupDisconnectedRoom() {
        Instant now = Instant.now();
        long nowMs = System.currentTimeMillis();

        // ═══ 1. HOST grace period → đóng phòng nếu quá hạn ═══
        Iterator<Map.Entry<String, Instant>> hostIt = hostDisconnectTimes.entrySet().iterator();

        while (hostIt.hasNext()) {
            Map.Entry<String, Instant> entry = hostIt.next();
            String roomCode = entry.getKey();
            long elapsed = Duration.between(entry.getValue(), now).getSeconds();

            if (elapsed >= HOST_GRACE_PERIOD_SECONDS) {
                hostIt.remove();
                if (roomRedis.roomExists(roomCode)) {
                    log.info("Room {} auto-closed: host disconnected {}s (limit: {}s)",
                            roomCode, elapsed, HOST_GRACE_PERIOD_SECONDS);
                    closeRoom(roomCode, "Host ngắt kết nối quá " + HOST_GRACE_PERIOD_SECONDS + " giây");
                }
            }
        }

        // ═══ 2. MEMBER grace period → xóa member nếu quá hạn ═══
        Iterator<Map.Entry<String, Instant>> memberIt = memberDisconnectTimes.entrySet().iterator();
        while (memberIt.hasNext()) {
            Map.Entry<String, Instant> entry = memberIt.next();
            String key = entry.getKey();
            long elapsed = Duration.between(entry.getValue(), now).getSeconds();

            if (elapsed >= MEMBER_GRACE_PERIOD_SECONDS) {
                memberIt.remove();

                String[] parts = key.split(":", 2);
                String roomCode = parts[0];
                Long userId = Long.parseLong(parts[1]);

                // Room đã đóng → skip
                if (!roomRedis.roomExists(roomCode)) continue;
                // User đã không còn là member → skip
                if (!roomRedis.isMember(roomCode, userId)) continue;

                // Remove member
                roomRedis.removeMember(roomCode, userId);
                roomRedis.removeHeartbeat(roomCode, userId);

                User user = userRepository.findById(userId).orElse(null);
                String name = user != null ? getDisplayName(user) : "User";
                roomChatService.sendSystemMessage(roomCode, name + " đã rời phòng (mất kết nối)");
                broadcastMembers(roomCode);

                log.info("Member {} auto-removed from room {}: disconnected {}s",
                        userId, roomCode, elapsed);
            }
        }

        // ═══ 3. Cleanup orphaned rooms ═══
        Set<String> activeCodes = roomRedis.getActiveRoomCodes();
        for (String code : activeCodes) {
            if (!roomRedis.roomExists(code)) {
                roomRedis.deleteRoom(code);
                hostDisconnectTimes.remove(code);
                log.info("Cleaned up orphaned room: {}", code);
                continue;
            }
            // Room có 0 thành viên → đóng
            if (roomRedis.getMemberCount(code) == 0) {
                log.info("Room {} auto-closed: no members", code);
                closeRoom(code, "Phòng không còn thành viên");
            }

            String hostId = roomRedis.getRoomField(code, "hostId");
            Set<String> members = roomRedis.getMembers(code);

            for (String memberId : members) {
                Long userId = Long.parseLong(memberId);
                Long lastHb = roomRedis.getLastHeartbeat(code, userId);

                // Chưa có heartbeat (vừa join, chưa gửi heartbeat lần đầu) → skip
                if (lastHb == null) continue;

                long elapsedSeconds = (nowMs - lastHb) / 1000;
                if (elapsedSeconds < MEMBER_GRACE_PERIOD_SECONDS) continue;

                // Heartbeat expired
                if (userId.toString().equals(hostId)) {
                    // Host heartbeat expired → đóng phòng
                    log.info("Room {} auto-closed: host heartbeat expired ({}s)",
                            code, elapsedSeconds);
                    closeRoom(code, "Host ngắt kết nối quá " + HOST_GRACE_PERIOD_SECONDS + " giây");
                    break;
                } else {
                    roomRedis.removeMember(code, userId);
                    roomRedis.removeHeartbeat(code, userId);
                    memberDisconnectTimes.remove(memberKey(code, userId));

                    User user = userRepository.findById(userId).orElse(null);

                    String name = user != null ? getDisplayName(user) : "User";
                    roomChatService.sendSystemMessage(code, name + " đã rời phòng (mất kết nối)");
                    broadcastMembers(code);

                    log.info("Member {} removed from room {} (heartbeat expired: {}s)",
                            userId, code, elapsedSeconds);
                }
            }
        }
    }

    // ==================== ADMIN OPERATIONS ====================

    /**
     * Thống kê tổng quan + danh sách phòng (có search)
     */
    public RoomStatsResponse getRoomStats(String search) {
        Set<String> codes = roomRedis.getActiveRoomCodes();
        Instant now = Instant.now();

        List<RoomStatsResponse.RoomSummary> summaries = new ArrayList<>();

        for (String code : codes) {
            try {
                Map<Object, Object> data = roomRedis.getRoom(code);
                if (data.isEmpty()) {
                    roomRedis.deleteRoom(code);
                    continue;
                }

                Instant createdAt = Instant.parse(getStr(data, "createdAt", now.toString()));

                RoomStatsResponse.RoomSummary summary = RoomStatsResponse.RoomSummary.builder()
                        .code(code)
                        .roomName(getStr(data, "roomName"))
                        .hostUsername(getStr(data, "hostUsername"))
                        .hostFullName(getStr(data, "hostFullName"))
                        .hostAvatarUrl(getStr(data, "hostAvatarUrl"))
                        .movieName(getStr(data, "movieName"))
                        .movieSlug(getStr(data, "movieSlug"))
                        .moviePosterUrl(getStr(data, "moviePosterUrl"))
                        .state(getStr(data, "state"))
                        .memberCount((int) roomRedis.getMemberCount(code))
                        .maxMembers(Integer.parseInt(getStr(data, "maxMembers", "999")))
                        .requireApproval(Boolean.parseBoolean(getStr(data, "requireApproval", "true")))
                        .pendingCount((int) roomRedis.getPendingCount(code))
                        .chatMessageCount(roomRedis.getChatMessageCount(code))
                        .durationMinutes(Duration.between(createdAt, now).toMinutes())
                        .hostDisconnected(hostDisconnectTimes.containsKey(code))
                        .createdAt(createdAt)
                        .build();

                summaries.add(summary);
            } catch (Exception e) {
                log.error("Error building stats for room {}", code, e);
            }
        }

        // Search/filter
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase().trim();
            summaries = summaries.stream()
                    .filter(r ->
                            contains(r.getCode(), q) ||
                                    contains(r.getRoomName(), q) ||
                                    contains(r.getHostUsername(), q) ||
                                    contains(r.getHostFullName(), q) ||
                                    contains(r.getMovieName(), q))
                    .collect(Collectors.toList());
        }

        // Sort: member count giảm dần
        summaries.sort((a, b) -> b.getMemberCount() - a.getMemberCount());

        // Tính tổng
        int totalUsers = summaries.stream().mapToInt(RoomStatsResponse.RoomSummary::getMemberCount).sum();
        int totalRooms = summaries.size();
        long playing = summaries.stream().filter(r -> "PLAYING".equals(r.getState())).count();
        long waiting = totalRooms - playing;
        double avg = totalRooms > 0 ? (double) totalUsers / totalRooms : 0;

        return RoomStatsResponse.builder()
                .totalActiveRooms(totalRooms)
                .totalOnlineUsers(totalUsers)
                .avgMembersPerRoom(Math.round(avg * 100.0) / 100.0)
                .roomsPlaying((int) playing)
                .roomsWaiting((int) waiting)
                .rooms(summaries)
                .build();
    }

    /**
     * Chi tiết 1 phòng cho admin (bao gồm chat history, pending requests)
     */
    public RoomAdminDetailResponse getAdminRoomDetail(String code) {
        validateRoomExists(code);

        Map<Object, Object> data = roomRedis.getRoom(code);
        if (data.isEmpty()) throw new CommonMessageException("Phòng không tồn tại");

        Instant now = Instant.now();
        Instant createdAt = Instant.parse(getStr(data, "createdAt", now.toString()));

        // Movie info
        RoomResponse.RoomMovieDTO movie = null;
        String movieSlug = getStr(data, "movieSlug");
        if (movieSlug != null && !movieSlug.isBlank()) {
            movie = RoomResponse.RoomMovieDTO.builder()
                    .slug(movieSlug)
                    .name(getStr(data, "movieName"))
                    .posterUrl(getStr(data, "moviePosterUrl"))
                    .thumbUrl(getStr(data, "movieThumbUrl"))
                    .currentEpisode(getStr(data, "currentEpisode"))
                    .currentServerIndex(Integer.parseInt(getStr(data, "currentServerIndex", "0")))
                    .videoUrl(getStr(data, "videoUrl"))
                    .build();
        }

        // Chat history
        List<ChatMessageDTO> chatHistory = roomRedis.getChatHistory(code);

        // Pending requests
        List<JoinRequestDTO> pendingRequests = roomRedis.getPendingRequests(code);

        return RoomAdminDetailResponse.builder()
                .code(code)
                .roomName(getStr(data, "roomName"))
                .host(RoomResponse.RoomHostDTO.builder()
                        .id(Long.parseLong(getStr(data, "hostId")))
                        .fullName(getStr(data, "hostFullName"))
                        .username(getStr(data, "hostUsername"))
                        .avatarUrl(getStr(data, "hostAvatarUrl"))
                        .build())
                .movie(movie)
                .state(getStr(data, "state"))
                .currentTime(parseDoubleSafe(getStr(data, "currentTime"), 0))
                .speed(parseDoubleSafe(getStr(data, "speed"), 1))
                .memberCount((int) roomRedis.getMemberCount(code))
                .maxMembers(Integer.parseInt(getStr(data, "maxMembers", "999")))
                .requireApproval(Boolean.parseBoolean(getStr(data, "requireApproval", "true")))
                .members(buildMemberList(code))
                .pendingCount(pendingRequests.size())
                .pendingRequests(pendingRequests)
                .chatMessageCount(roomRedis.getChatMessageCount(code))
                .chatHistory(chatHistory)
                .durationMinutes(Duration.between(createdAt, now).toMinutes())
                .hostDisconnected(hostDisconnectTimes.containsKey(code))
                .createdAt(createdAt)
                .build();
    }

    /**
     * Admin đóng phòng bất kỳ
     */
    public void adminCloseRoom(String code) {
        validateRoomExists(code);
        closeRoom(code, "Phòng đã bị đóng bởi Quản trị viên");
        log.info("Room {} force-closed by admin", code);
    }

    /**
     * Admin kick user (không cần là host, không cần join phòng)
     */
    public void adminKickUser(String code, Long userId) {
        validateRoomExists(code);

        if (!roomRedis.isMember(code, userId))
            throw new CommonMessageException("User không ở trong phòng này");

        String hostId = roomRedis.getRoomField(code, "hostId");
        if (userId.toString().equals(hostId))
            throw new CommonMessageException("Không thể kick host khỏi phòng");

        performKick(code, userId, true);
    }

    /**
     * Admin gửi tin nhắn vào phòng (không cần join)
     */
    public void adminSendMessage(String code, ChatSendDTO dto) {
        validateRoomExists(code);
        User admin = userService.getCurrentUser();
        roomChatService.sendAdminMessage(code, dto, admin);
    }


    // ==================== PRIVATE HELPERS ====================

    /**
     * Thực hiện kick — dùng chung cho host kick VÀ admin kick
     */
    private void performKick(String code, Long userId, boolean byAdmin) {
        User kickUser = userRepository.findById(userId).orElse(null);
        String kickName = kickUser != null ? getDisplayName(kickUser) : "User";

        roomRedis.removeMember(code, userId);
        roomRedis.removeHeartbeat(code, userId);

        memberDisconnectTimes.remove(memberKey(code, userId));
        messagingTemplate.convertAndSend("/topic/room/" + code + "/kicked",
                Map.of("userId", userId));
        String suffix = byAdmin ? " đã bị kick bởi Admin" : " đã bị kick khỏi phòng";
        roomChatService.sendSystemMessage(code, kickName + suffix);
        broadcastMembers(code);
        log.info("User {} kicked from room {} (byAdmin: {})", userId, code, byAdmin);
    }

    private void validateHostById(String code, Long userId) {
        String hostId = roomRedis.getRoomField(code, "hostId");
        if (!userId.toString().equals(hostId))
            throw new CommonMessageException("Chỉ chủ phòng mới có quyền thực hiện");
    }

    private void validateHostOrAdmin(String code) {
        if (SecurityUtil.isAdmin()) return;

        User currentUser = userService.getCurrentUser();
        String hostId = roomRedis.getRoomField(code, "hostId");
        if (!currentUser.getId().toString().equals(hostId))
            throw new CommonMessageException("Chỉ chủ phòng hoặc admin mới có quyền thực hiện");
    }

    private void addMemberToRoom(String code, User user) {
        roomRedis.addMember(code, user.getId());
        roomRedis.updateHeartbeat(code, user.getId());
        broadcastMembers(code);

        String displayName = getDisplayName(user);
        roomChatService.sendSystemMessage(code, displayName + " đã tham gia phòng");
        log.info("User {} joined room {}", user.getUsername(), code);
    }

    private void closeRoom(String code, String reason) {
        messagingTemplate.convertAndSend("/topic/room/" + code + "/closed", Map.of("reason", reason));

        Set<String> members = roomRedis.getMembers(code);
        for (String memberId : members) {
            Long uid = Long.parseLong(memberId);
            roomRedis.removeUserRoom(uid);
            memberDisconnectTimes.remove(memberKey(code, uid));
        }

        roomRedis.deleteRoom(code);
        hostDisconnectTimes.remove(code);
        log.info("Room {} closed: {}", code, reason);
    }

    private void broadcastMembers(String code) {
        List<RoomMemberDTO> members = buildMemberList(code);
        messagingTemplate.convertAndSend("/topic/room/" + code + "/members", members);
    }

    private List<RoomMemberDTO> buildMemberList(String code) {
        Set<String> memberIds = roomRedis.getMembers(code);
        String hostId = roomRedis.getRoomField(code, "hostId");

        List<Long> ids = memberIds.stream().map(Long::parseLong).collect(Collectors.toList());
        List<User> users = userRepository.findAllById(ids);

        return users.stream()
                .map(user -> RoomMemberDTO.builder()
                        .userId(user.getId())
                        .userName(user.getUsername())
                        .fullName(getDisplayName(user))
                        .avatarUrl(user.getAvatarUrl())
                        .isHost(user.getId().toString().equals(hostId))
                        .build())
                .sorted((a, b) -> Boolean.compare(Boolean.TRUE.equals(b.getIsHost()), Boolean.TRUE.equals(a.getIsHost())))
                .collect(Collectors.toList());
    }

    private RoomResponse buildRoomResponse(String code) {
        Map<Object, Object> data = roomRedis.getRoom(code);

        if (data.isEmpty()) throw new CommonMessageException("Phòng không tồn tại");

        RoomResponse.RoomMovieDTO movie = null;
        String movieSlug = getStr(data, "movieSlug");
        if (movieSlug != null && !movieSlug.isBlank()) {
            movie = RoomResponse.RoomMovieDTO.builder()
                    .slug(movieSlug)
                    .name(getStr(data, "movieName"))
                    .posterUrl(getStr(data, "moviePosterUrl"))
                    .thumbUrl(getStr(data, "movieThumbUrl"))
                    .currentEpisode(getStr(data, "currentEpisode"))
                    .currentServerIndex(Integer.parseInt(getStr(data, "currentServerIndex", "0")))
                    .videoUrl(getStr(data, "videoUrl"))
                    .build();
        }

        return RoomResponse.builder()
                .code(code)
                .roomName(getStr(data, "roomName"))
                .host(RoomResponse.RoomHostDTO.builder()
                        .id(Long.parseLong(getStr(data, "hostId")))
                        .fullName(getStr(data, "hostFullName"))
                        .username(getStr(data, "hostUsername"))
                        .avatarUrl(getStr(data, "hostAvatarUrl"))
                        .build())
                .movie(movie)
                .state(getStr(data, "state"))
                .currentTime(parseDoubleSafe(getStr(data, "currentTime"), 0))
                .speed(parseDoubleSafe(getStr(data, "speed"), 1))
                .memberCount((int) roomRedis.getMemberCount(code))
                .maxMembers(Integer.parseInt(getStr(data, "maxMembers", "10")))
                .requireApproval(Boolean.parseBoolean(getStr(data, "requireApproval", "true")))
                .members(buildMemberList(code))
                .pendingCount((int) roomRedis.getPendingCount(code))
                .suggestions(roomRedis.getSuggestions(code))
                .createdAt(Instant.parse(getStr(data, "createdAt", Instant.now().toString())))
                .build();
    }

    private double parseDoubleSafe(String value, double defaultValue) {
        if (value == null || value.isBlank()) return defaultValue;

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void validateRoomExists(String code) {
        if (!roomRedis.roomExists(code)) throw new CommonMessageException("Phòng không tồn tại hoặc đã đóng");
    }

    private String memberKey(String roomCode, Long userId) {
        return roomCode + ":" + userId;
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private void validateHost(String code) {
        User currentUser = userService.getCurrentUser();
        validateHost(code, currentUser);
    }

    private void validateHost(String code, User user) {
        String hostId = roomRedis.getRoomField(code, "hostId");
        if (!user.getId().toString().equals(hostId))
            throw new CommonMessageException("Chỉ chủ phòng mới có quyền thực hiện");
    }

    private String getStr(Map<Object, Object> map, String key) {
        Object val = map.get(key);

        return val != null ? val.toString() : null;
    }

    private String getStr(Map<Object, Object> map, String key, String defaultVal) {
        Object val = map.get(key);

        return val != null ? val.toString() : defaultVal;
    }

    private String getDisplayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }

        return user.getUsername();
    }
}
