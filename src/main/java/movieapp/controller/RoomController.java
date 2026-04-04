package movieapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import movieapp.dto.Room.*;
import movieapp.dto.Room.Chat.ChatMessageDTO;
import movieapp.dto.Room.Chat.ChatSendDTO;
import movieapp.service.RoomChatService;
import movieapp.service.RoomService;
import movieapp.util.annotation.ApiMessage;
import movieapp.util.annotation.RequireAdmin;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;
    private final RoomChatService roomChatService;

    @PostMapping
    @ApiMessage("Tạo phòng xem chung")
    public RoomResponse createRoom(@Valid @RequestBody RoomCreateDTO dto) {
        return roomService.createRoom(dto);
    }

    @GetMapping("/{code}")
    @ApiMessage("Lấy thông tin phòng")
    public RoomResponse getRoom(@PathVariable String code) {
        return roomService.getRoomDetails(code);
    }

    @PostMapping("/{code}/join")
    @ApiMessage("Tham gia phòng")
    public RoomResponse joinRoom(@PathVariable String code) {
        return roomService.joinRoom(code);
    }

    @PostMapping("/{code}/leave")
    @ApiMessage("Rời phòng")
    public Void leaveRoom(@PathVariable String code) {
        roomService.leaveRoom(code);

        return null;
    }

    @PostMapping("/{code}/approve/{userId}")
    @ApiMessage("Duyệt yêu cầu vào phòng")
    public Void approveRequest(@PathVariable String code, @PathVariable Long userId) {
        roomService.approveRequest(code, userId);

        return null;
    }

    @PostMapping("/{code}/reject/{userId}")
    @ApiMessage("Từ chối yêu cầu vào phòng")
    public Void rejectRequest(@PathVariable String code, @PathVariable Long userId) {
        roomService.rejectRequest(code, userId);

        return null;
    }

    @PostMapping("/{code}/kick/{userId}")
    @ApiMessage("Kick user khỏi phòng")
    public Void kickUser(@PathVariable String code, @PathVariable Long userId) {
        roomService.kickUser(code, userId);

        return null;
    }

    @PutMapping("/{code}/movie")
    @ApiMessage("Chọn phim cho phòng")
    public Void setMovie(@PathVariable String code, @RequestBody RoomResponse.RoomMovieDTO movie) {
        roomService.setMovie(code, movie);

        return null;
    }

    @PutMapping("/{code}/settings")
    @ApiMessage("Cập nhật cài đặt phòng")
    public Void updateSettings(@PathVariable String code, @RequestBody Map<String, Object> settings) {
        roomService.updateSettings(code, settings);

        return null;
    }

    @GetMapping("/browse")
    @ApiMessage("Danh sách phòng đang hoạt động")
    public List<RoomBrowseResponse> browseRooms() {
        return roomService.getActiveRooms();
    }

    @GetMapping("/{code}/chat/history")
    @ApiMessage("Lấy lịch sử chat")
    public List<ChatMessageDTO> getChatHistory(@PathVariable String code) {
        return roomChatService.getChatHistory(code);
    }

    @GetMapping("/{code}/current-time")
    @ApiMessage("Lấy thời gian hiện tại của chủ phòng")
    public RoomCurrentTimeResponse getCurrentTime(@PathVariable String code) {
        return roomService.getCurrentTime(code);
    }

    // ==================== ADMIN ENDPOINTS ====================
    @GetMapping("/admin/stats")
    @RequireAdmin
    @ApiMessage("Thống kê phòng xem chung")
    public RoomStatsResponse getRoomStats(
            @RequestParam(required = false) String search) {
        return roomService.getRoomStats(search);
    }

    @GetMapping("/admin/{code}")
    @RequireAdmin
    @ApiMessage("Chi tiết phòng xem chung (Admin)")
    public RoomAdminDetailResponse getAdminRoomDetail(@PathVariable String code) {
        return roomService.getAdminRoomDetail(code);
    }

    @DeleteMapping("/admin/{code}")
    @RequireAdmin
    @ApiMessage("Đóng phòng xem chung (Admin)")
    public Void adminCloseRoom(@PathVariable String code) {
        roomService.adminCloseRoom(code);
        return null;
    }

    @PostMapping("/admin/{code}/kick/{userId}")
    @RequireAdmin
    @ApiMessage("Admin kick user khỏi phòng")
    public Void adminKickUser(@PathVariable String code, @PathVariable Long userId) {
        roomService.adminKickUser(code, userId);
        return null;
    }

    @PostMapping("/admin/{code}/chat")
    @RequireAdmin
    @ApiMessage("Admin gửi tin nhắn vào phòng")
    public Void adminSendMessage(@PathVariable String code,
                                 @RequestBody ChatSendDTO dto) {
        roomService.adminSendMessage(code, dto);
        return null;
    }

    @DeleteMapping("/admin/{code}/chat/{messageId}")
    @RequireAdmin
    @ApiMessage("Admin xóa tin nhắn")
    public Void adminDeleteMessage(@PathVariable String code,
                                   @PathVariable String messageId) {
        roomChatService.adminDeleteMessage(code, messageId);
        return null;
    }
}
