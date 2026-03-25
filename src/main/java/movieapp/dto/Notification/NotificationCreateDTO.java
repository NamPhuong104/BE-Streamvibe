package movieapp.dto.Notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import movieapp.util.constant.NotificationSeverity;
import movieapp.util.constant.NotificationTarget;
import movieapp.util.constant.NotificationType;

import java.time.Instant;

@Data
public class NotificationCreateDTO {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    private String message;

    @NotNull(message = "Loại thông báo không được để trống")
    private NotificationType type;

    private NotificationTarget target;

    private NotificationSeverity severity;

    private String imageUrl;
    private String actionUrl;
    private String actionText;

    private Instant startAt;
    private Instant endAt;
}
