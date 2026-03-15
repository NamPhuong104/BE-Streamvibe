package movieapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.dto.Room.Chat.ChatDeleteDTO;
import movieapp.dto.Room.Chat.ChatMessageDTO;
import movieapp.dto.Room.Chat.ChatSendDTO;
import movieapp.dto.Room.Chat.ChatTypingDTO;
import movieapp.entity.User;
import movieapp.exception.CommonMessageException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomChatService {
    private final RoomRedisService roomRedis;
    private final WsUserCacheService wsUserCache;
    private final SimpMessagingTemplate messagingTemplate;

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int REPLY_PREVIEW_LENGTH = 50;

    // ==================== GỬI TIN NHẮN ====================

    /**
     * Xử lý khi user gửi tin nhắn
     * <p>
     * Flow:
     * 1. Validate: user phải là member, content không rỗng, rate limit
     * 2. Nếu có replyToId → tìm tin nhắn gốc trong Redis
     * 3. Build ChatMessageDTO đầy đủ
     * 4. Lưu vào Redis List
     * 5. Broadcast tới tất cả members qua STOMP
     */
    public void sendMessage(String code, ChatSendDTO dto, Principal principal) {
        User user = wsUserCache.getFullUser(principal);

        validateSendMessage(code, user, dto);

        ChatMessageDTO message = buildUserMessage(code, user, dto);

        roomRedis.addChatMessage(code, message);

        messagingTemplate.convertAndSend("/topic/room/" + code + "/chat", message);

        log.debug("Chat message sent in room {} by {}", code, user.getUsername());
    }

    // ==================== XÓA TIN NHẮN ====================

    /**
     * Xử lý khi user xóa tin nhắn
     * <p>
     * Quyền xóa:
     * - User xóa tin nhắn CỦA MÌNH → luôn được
     * - Host xóa tin nhắn CỦA NGƯỜI KHÁC → được (moderation)
     * - Member xóa tin nhắn CỦA NGƯỜI KHÁC → KHÔNG được
     */
    public void deleteMessage(String code, ChatDeleteDTO dto, Principal principal) {
        User user = wsUserCache.getFullUser(principal);

        if (!roomRedis.isMember(code, user.getId())) throw new CommonMessageException("Bạn không ở trong phòng này");

        ChatMessageDTO message = roomRedis.findChatMessageById(code, dto.getMessageId());
        if (message == null) return;
        if (Boolean.TRUE.equals(message.getDeleted())) return;

        String hostId = roomRedis.getRoomField(code, "hostId");
        boolean isHost = user.getId().toString().equals(hostId);
        boolean isOwner = user.getId().equals(message.getSenderId());

        if (!isOwner && !isHost) throw new CommonMessageException("Bạn không có quyền xóa tin nhắn này");

        boolean deleted = roomRedis.softDeleteChatMessage(code, dto.getMessageId());

        if (deleted) {
            messagingTemplate.convertAndSend("/topic/room/" + code + "/chat/delete", dto);
            log.debug("Message {} deleted in room {} by {}",
                    dto.getMessageId(), code, user.getUsername());
        }
    }

    // ==================== TYPING INDICATOR ====================

    /**
     * Xử lý typing indicator
     * <p>
     * KHÔNG lưu Redis - chỉ broadcast realtime
     * Vì typing status là ephemeral (tạm thời),
     * không ai cần xem lại "ai đã từng typing"
     */
    public void handleTyping(String code, Principal principal) {
        User user = wsUserCache.getFullUser(principal);

        if (!roomRedis.isMember(code, user.getId())) return;

        ChatTypingDTO typing = ChatTypingDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(getDisplayName(user))
                .isTyping(true)
                .build();

        messagingTemplate.convertAndSend("/topic/room/" + code + "/chat/typing", typing);
    }

    // ==================== SYSTEM MESSAGES ====================

    /**
     * Gửi tin nhắn hệ thống (join, leave, episode change...)
     * <p>
     * System messages:
     * - type = "SYSTEM"
     * - Không có sender info (senderId = null)
     * - Lưu Redis để scroll lên thấy lại
     * - Không bị rate limit
     */
    public void sendSystemMessage(String code, String content) {
        Double videoTimestamp = getCurrentVideoTimestamp(code);

        ChatMessageDTO systemMsg = ChatMessageDTO.builder()
                .id(UUID.randomUUID().toString())
                .type("SYSTEM")
                .content(content)
                .videoTimestamp(videoTimestamp)
                .createdAt(System.currentTimeMillis())
                .deleted(false)
                .build();

        roomRedis.addChatMessage(code, systemMsg);

        messagingTemplate.convertAndSend("/topic/room/" + code + "/chat", systemMsg);
    }

    // ==================== CHAT HISTORY ====================

    /**
     * Lấy lịch sử chat khi user join phòng
     * <p>
     * Trả về toàn bộ messages từ đầu phòng
     * Client sẽ scroll lên để xem tin cũ
     */

    public List<ChatMessageDTO> getChatHistory(String code) {
        if (!roomRedis.roomExists(code)) throw new CommonMessageException("Phòng không tồn tại");

        return roomRedis.getChatHistory(code);
    }

    // ==================== PRIVATE HELPERS ====================
    private void validateSendMessage(String code, User user, ChatSendDTO dto) {
        // Check phòng tồn tại
        if (!roomRedis.roomExists(code)) throw new CommonMessageException("Phòng không tồn tại");

        // Check member
        if (!roomRedis.isMember(code, user.getId())) throw new CommonMessageException("Bạn không ở trong phòng này");

        // Check content
        if (dto.getContent() == null || dto.getContent().trim().isEmpty())
            throw new CommonMessageException("Tin nhắn không được để trống");

        if (dto.getContent().length() > MAX_MESSAGE_LENGTH)
            throw new CommonMessageException("Tin nhắn tối đa " + MAX_MESSAGE_LENGTH + " ký tự");

        if (roomRedis.isChatRateLimited(code, user.getId()))
            throw new CommonMessageException("Bạn đang gửi tin nhắn quá nhanh. Vui lòng chờ 2 giây.");
    }

    /**
     * Build tin nhắn USER đầy đủ
     */
    private ChatMessageDTO buildUserMessage(String code, User user, ChatSendDTO dto) {
        ChatMessageDTO.ChatMessageDTOBuilder builder = ChatMessageDTO.builder()
                .id(UUID.randomUUID().toString())
                .type("USER")
                .senderId(user.getId())
                .senderUsername(user.getUsername())
                .senderFullName(getDisplayName(user))
                .senderAvatarUrl(user.getAvatarUrl())
                .content(dto.getContent().trim())
                .videoTimestamp(getCurrentVideoTimestamp(code))
                .createdAt(System.currentTimeMillis())
                .deleted(false);

        // Handle reply
        if (dto.getReplyToId() != null && !dto.getReplyToId().isBlank()) {
            ChatMessageDTO original = roomRedis.findChatMessageById(code, dto.getReplyToId());

            if (original != null && !Boolean.TRUE.equals(original.getDeleted())) {

                builder.replyToId(original.getId());

                // Cắt nội dung reply tối đa 50 ký tự
                String preview = original.getContent();
                if (preview != null && preview.length() > REPLY_PREVIEW_LENGTH)
                    preview = preview.substring(0, REPLY_PREVIEW_LENGTH) + "...";

                builder.replyToContent(preview);
                builder.replyToSenderName(original.getSenderFullName() != null ? original.getSenderFullName() : original.getSenderUsername());
            }
        }

        return builder.build();
    }

    /**
     * Lấy thời gian video hiện tại (nếu đang phát)
     * <p>
     * Nếu room state = "PLAYING" → trả về currentTime
     * Nếu state = "WAITING" → trả về null (chưa phát/đang pause)
     */
    private Double getCurrentVideoTimestamp(String code) {
        String state = roomRedis.getRoomField(code, "state");
        if ("PLAYING".equals(state)) {
            String timeStr = roomRedis.getRoomField(code, "currentTime");
            if (timeStr != null && !timeStr.isBlank()) {
                try {
                    double time = Double.parseDouble(timeStr);

                    return time > 0 ? time : null;
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * Lấy tên hiển thị: ưu tiên fullName, fallback username
     */
    private String getDisplayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) return user.getFullName();

        return user.getUsername();
    }
}
