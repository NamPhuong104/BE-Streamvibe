package movieapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import movieapp.dto.Notification.NotificationCreateDTO;
import movieapp.dto.Notification.NotificationResponse;
import movieapp.service.NotificationService;
import movieapp.util.annotation.ApiMessage;
import movieapp.util.annotation.RequireAdmin;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    // ==================== PUBLIC ====================
    @GetMapping("/active")
    @ApiMessage("Lấy thông báo đang hoạt động")
    public List<NotificationResponse> getActiveNotifications() {
        return notificationService.getActiveNotifications();
    }

    // ==================== ADMIN ====================
    @GetMapping
    @RequireAdmin
    @ApiMessage("Lấy tất cả thông báo")
    public List<NotificationResponse> getAllNotifications() {
        return notificationService.getAllNotifications();
    }

    @PostMapping
    @RequireAdmin
    @ApiMessage("Tạo thông báo mới")
    public NotificationResponse createNotification(@Valid @RequestBody NotificationCreateDTO dto) {
        return notificationService.createNotification(dto);
    }

    @PutMapping("/{id}")
    @RequireAdmin
    @ApiMessage("Cập nhật thông báo")
    public NotificationResponse updateNotification(@PathVariable Long id, @Valid @RequestBody NotificationCreateDTO dto) {
        return notificationService.updateNotification(id, dto);
    }

    @DeleteMapping("/{id}")
    @RequireAdmin
    @ApiMessage("Xóa thông báo")
    public Void deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);

        return null;
    }

    @PatchMapping("/{id}/toggle")
    @RequireAdmin
    @ApiMessage("Bật/tắt thông báo")
    public NotificationResponse toggleActive(@PathVariable Long id) {
        return notificationService.toggleActive(id);
    }

    @PostMapping("/{id}/push")
    @RequireAdmin
    @ApiMessage("Gửi thông báo thủ công")
    public Void pushManually(@PathVariable Long id) {
        notificationService.pushManually(id);

        return null;
    }
}
