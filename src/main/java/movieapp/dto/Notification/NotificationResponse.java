package movieapp.dto.Notification;

import lombok.Builder;
import lombok.Data;
import movieapp.util.constant.NotificationSeverity;
import movieapp.util.constant.NotificationTarget;
import movieapp.util.constant.NotificationType;

import java.time.Instant;

@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private NotificationTarget target;
    private String imageUrl;
    private String actionUrl;
    private String actionText;
    private NotificationSeverity severity;
    private Instant startAt;
    private Instant endAt;
    private boolean active;
    private boolean pushed;
    private String createdByName;
    private Instant createdAt;
}
