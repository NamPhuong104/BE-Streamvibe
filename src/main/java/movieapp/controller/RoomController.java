package movieapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import movieapp.dto.Room.RoomBrowseResponse;
import movieapp.dto.Room.RoomCreateDTO;
import movieapp.dto.Room.RoomResponse;
import movieapp.service.RoomService;
import movieapp.util.annotation.ApiMessage;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;

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
}
