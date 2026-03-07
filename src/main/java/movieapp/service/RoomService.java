package movieapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.dto.Room.*;
import movieapp.entity.User;
import movieapp.exception.CommonMessageException;
import movieapp.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {
    private static final int DEFAULT_MAX_MEMBERS = 999;
    private static final long HOST_GRACE_PERIOD_SECONDS = 300; // 5 minutes
    private final RoomRedisService roomRedis;
    private final UserRepository userRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    // Track host disconnect time for grace period
    private final Map<String, Instant> hostDisconnectTimes = new java.util.concurrent.ConcurrentHashMap<>();

    // ==================== CREATE ROOM ====================
    public RoomResponse createRoom(RoomCreateDTO dto) {
        User currentUser = userService.getCurrentUser();
        String existingRoom = roomRedis.getUserRoom(currentUser.getId());
        if (existingRoom != null)
            throw new CommonMessageException("Bạn đang ở phòng " + existingRoom + ". Vui lòng rời phòng trước.");

        boolean isAdmin = currentUser.getRole().getName().contains("ADMIN");

        String code = roomRedis.generateUniqueCode();

        String roomName = dto.getRoomName() != null && !dto.getRoomName().isBlank() ? dto.getRoomName() : "Phòng của " + currentUser.getUsername();

        // Create room data
        Map<String, String> roomData = new HashMap<>();
        roomData.put("code", code);
        roomData.put("hostId", currentUser.getId().toString());
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
        ;

        roomRedis.createRoom(code, roomData);

        // Add host as first member
        roomRedis.addMember(code, currentUser.getId());
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

        if (requireApproval) {
            JoinRequestDTO request = JoinRequestDTO.builder()
                    .userId(currentUser.getId())
                    .username(currentUser.getUsername())
                    .avatarUrl(currentUser.getAvatarUrl())
                    .requestedAt(Instant.now().toEpochMilli())
                    .build();

            roomRedis.addPendingRequest(code, request);

            messagingTemplate.convertAndSend("/topic/room/" + code + "/request", request);

            throw new CommonMessageException("Yêu cầu đã được gửi. Chờ host duyệt.");
        }

        addMemberToRoom(code, currentUser);

        return buildRoomResponse(code);
    }

    // ==================== APPROVE/REJECT REQUEST ====================
    public void approveRequest(String code, Long userId) {
        validateHost(code);

        User user = userRepository.findById(userId).orElseThrow(() -> new CommonMessageException("User không tồn tại"));

        roomRedis.removePendingRequest(code, userId);
        addMemberToRoom(code, user);

        messagingTemplate.convertAndSend("/topic/room/" + code + "/approved", Map.of("userId", userId, "approved", true));
    }

    public void rejectRequest(String code, Long userId) {
        validateHost(code);
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
            broadcastMembers(code);
            log.info("User {} left room {}", currentUser.getUsername(), code);
        }
    }

    // ==================== KICK USER ====================
    public void kickUser(String code, Long userId) {
        validateHost(code);

        String hostId = roomRedis.getRoomField(code, "hostId");
        if (userId.toString().equals(hostId)) throw new CommonMessageException("Host không thể tự kick chính mình");

        roomRedis.removeMember(code, userId);

        messagingTemplate.convertAndSend(
                "/topic/room/" + code + "/kicked",
                Map.of("userId", userId)
        );

        broadcastMembers(code);
        log.info("User {} kicked from room {}", userId, code);
    }

    // ==================== VIDEO SYNC ====================
    public void syncState(String code, RoomStateDTO state, Principal principal) {
        User currentUser = getUserFromWsPrincipal(principal);
        validateHost(code, currentUser);

        Map<String, String> updates = new HashMap<>();
        updates.put("currentTime", String.valueOf(state.getCurrentTime()));

        if (state.getVideoUrl() != null) updates.put("videoUrl", String.valueOf(state.getVideoUrl()));

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
        validateHost(code);

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

        roomRedis.clearSuggestions(code);

        messagingTemplate.convertAndSend("/topic/room/" + code + "/movie", movie);
    }

    public void suggestMovie(String code, MovieSuggestDTO suggestion, Principal principal) {
        User currentuser = getUserFromWsPrincipal(principal);
        validateRoomExists(code);

        if (!roomRedis.isMember(code, currentuser.getId()))
            throw new CommonMessageException("Bạn không ở trong phòng này");

        suggestion.setSuggestedByUserId(currentuser.getId());
        suggestion.setSuggestedByUsername(currentuser.getUsername());
        suggestion.setSuggestedAt(Instant.now().toEpochMilli());

        roomRedis.addSuggestion(code, suggestion);

        messagingTemplate.convertAndSend("/topic/room/" + code + "/suggest", suggestion);
    }

    // ==================== ROOM SETTINGS ====================
    public void updateSettings(String code, Map<String, Object> settings) {
        validateHost(code);

        Map<String, String> updates = new HashMap<>();

        if (settings.containsKey("requireApproval"))
            updates.put("requireApproval", settings.get("requireApproval").toString());

        if (settings.containsKey("maxMembers")) updates.put("maxMembers", settings.get("maxMembers").toString());

        if (settings.containsKey("roomName")) updates.put("roomName", settings.get("roomName").toString());

        if (!updates.isEmpty()) roomRedis.updateRoomFields(code, updates);
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

        String hostId = roomRedis.getRoomField(roomCode, "hostId");

        if (user.getId().toString().equals(hostId)) {
            // Host disconnected → start grace period
            hostDisconnectTimes.put(roomCode, Instant.now());

            log.info("Host {} disconnected from room {}. Grace period started.", user.getUsername(), roomCode);

            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/host-status", Map.of("status", "disconnected", "graceSeconds", HOST_GRACE_PERIOD_SECONDS));

        }
    }

    public void handleUserReconnect(String email) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) return;

        String roomCode = roomRedis.getUserRoom(user.getId());
        if (roomCode == null) return;

        // Cancel grace period if host reconnected
        hostDisconnectTimes.remove(roomCode);

        // Notify members host is back
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/host-status", Map.of("status", "connected"));

        log.info("User {} reconnected to room {}", user.getUsername(), roomCode);
    }

    // ==================== SCHEDULED: GRACE PERIOD CHECK ====================
    @Scheduled(fixedRate = 30000)
    public void checkGracePeriods() {
        Instant now = Instant.now();

        new HashMap<>(hostDisconnectTimes).forEach((roomCode, disconnectTime) -> {
            long elapsed = now.getEpochSecond() - disconnectTime.getEpochSecond();

            if (elapsed >= HOST_GRACE_PERIOD_SECONDS) {
                log.info("Grace period expired for room {}. Closing.", roomCode);
                closeRoom(roomCode, "Host không kết nối lại. Phòng đã đóng.");
                hostDisconnectTimes.remove(roomCode);
            }
        });
    }

    // ==================== PRIVATE HELPERS ====================
    private User getUserFromWsPrincipal(Principal principal) {
        if (principal instanceof JwtAuthenticationToken auth) {
            String email = auth.getToken().getSubject();

            return userRepository.findByEmail(email).orElseThrow(() -> new CommonMessageException("User không tồn tại"));
        }
        throw new CommonMessageException("Yêu cầu xác thực WebSocket");
    }

    private void addMemberToRoom(String code, User user) {
        roomRedis.addMember(code, user.getId());
        broadcastMembers(code);
        log.info("User {} joined room {}", user.getUsername(), code);
    }

    private void closeRoom(String code, String reason) {
        messagingTemplate.convertAndSend("/topic/room/" + code + "/closed", Map.of("reason", reason));

        Set<String> members = roomRedis.getMembers(code);
        for (String memberId : members)
            roomRedis.removeUserRoom(Long.parseLong(memberId));

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
                        .username(getStr(data, "hostUsername"))
                        .avatarUrl(getStr(data, "hostAvatarUrl"))
                        .build())
                .movie(movie)
                .state(getStr(data, "state"))
                .memberCount((int) roomRedis.getMemberCount(code))
                .maxMembers(Integer.parseInt(getStr(data, "maxMembers", "10")))
                .requireApproval(Boolean.parseBoolean(getStr(data, "requireApproval", "true")))
                .members(buildMemberList(code))
                .pendingCount((int) roomRedis.getPendingCount(code))
                .createdAt(Instant.parse(getStr(data, "createdAt", Instant.now().toString())))
                .build();
    }

    private void validateRoomExists(String code) {
        if (!roomRedis.roomExists(code)) throw new CommonMessageException("Phòng không tồn tại hoặc đã đóng");
    }

    private void validateHost(String code) {
        User currentUser = userService.getCurrentUser();
        validateHost(code, currentUser);
    }

    private void validateHost(String code, User user) {
        String hostId = roomRedis.getRoomField(code, "hostId");
        if (!user.getId().toString().equals(hostId))
            throw new CommonMessageException("Chỉ host mới có quyền thực hiện");
    }

    private String getStr(Map<Object, Object> map, String key) {
        Object val = map.get(key);

        return val != null ? val.toString() : null;
    }

    private String getStr(Map<Object, Object> map, String key, String defaultVal) {
        Object val = map.get(key);

        return val != null ? val.toString() : defaultVal;
    }
}
