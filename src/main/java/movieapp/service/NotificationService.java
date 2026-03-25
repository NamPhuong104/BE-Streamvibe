package movieapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieapp.dto.Notification.NotificationCreateDTO;
import movieapp.dto.Notification.NotificationResponse;
import movieapp.entity.Notification;
import movieapp.entity.User;
import movieapp.exception.CommonMessageException;
import movieapp.repository.NotificationRepository;
import movieapp.util.SecurityUtil;
import movieapp.util.constant.NotificationSeverity;
import movieapp.util.constant.NotificationTarget;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    // ==================== PUBLIC ====================

    /**
     * Lấy tất cả thông báo đang active
     * Frontend sẽ filter theo target (ALL / LOGGED_IN) ở client
     */
    public List<NotificationResponse> getActiveNotifications() {
        boolean isAuthenticated = SecurityUtil.isAuthenticated();
        ;

        List<NotificationTarget> targets = isAuthenticated ? List.of(NotificationTarget.ALL, NotificationTarget.LOGGED_IN) : List.of(NotificationTarget.ALL);

        return notificationRepository.findActiveByTargets(Instant.now(), targets).stream().map(this::convertToResponse).toList();
    }

    // ==================== ADMIN ====================
    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc().stream().map(this::convertToResponse).toList();
    }

    @Transactional
    public NotificationResponse createNotification(NotificationCreateDTO dto) {
        User user = userService.getCurrentUser();
        Notification notification = Notification.builder()
                .title(dto.getTitle())
                .message(dto.getMessage())
                .type(dto.getType())
                .target(dto.getTarget() != null ? dto.getTarget() : NotificationTarget.ALL)
                .imageUrl(dto.getImageUrl())
                .actionUrl(dto.getActionUrl())
                .actionText(dto.getActionText())
                .severity(dto.getSeverity() != null ? dto.getSeverity() : NotificationSeverity.INFO)
                .startAt(dto.getStartAt() != null ? dto.getStartAt() : Instant.now())
                .endAt(dto.getEndAt())
                .active(true)
                .pushed(false)
                .createdBy(user)
                .build();
        notification = notificationRepository.save(notification);

        // Push ngay nếu đang trong khoảng active
        if (isCurrentlyActive(notification)) pushViaWebsocket(notification);

        log.info("📢 Created notification: id={}, title='{}', type={}, startAt={}, endAt={}",
                notification.getId(), notification.getTitle(), notification.getType(),
                notification.getStartAt(), notification.getEndAt());

        return convertToResponse(notification);
    }

    @Transactional
    public NotificationResponse updateNotification(Long id, NotificationCreateDTO dto) {
        Notification notification = findById(id);

        notification.setTitle(dto.getTitle());
        notification.setMessage(dto.getMessage());
        notification.setType(dto.getType());
        if (dto.getTarget() != null) notification.setTarget(dto.getTarget());
        notification.setImageUrl(dto.getImageUrl());
        if (dto.getSeverity() != null) notification.setSeverity(dto.getSeverity());
        notification.setActionUrl(dto.getActionUrl());
        notification.setActionText(dto.getActionText());
        if (dto.getStartAt() != null) notification.setStartAt(dto.getStartAt());

        notification.setPushed(false);

        return convertToResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void deleteNotification(Long id) {
        if (!notificationRepository.existsById(id)) throw new CommonMessageException("Không tìm thấy thông báo");

        notificationRepository.deleteById(id);
    }

    @Transactional
    public NotificationResponse toggleActive(Long id) {
        Notification notification = findById(id);
        notification.setActive(!notification.isActive());

        return convertToResponse(notificationRepository.save(notification));
    }

    /**
     * Admin push thủ công - gửi lại qua WebSocket
     * User đã dismiss sẽ KHÔNG thấy lại (localStorage filter ở frontend)
     */
    @Transactional
    public void pushManually(Long id) {
        Notification notification = findById(id);
        if (!notification.isActive()) throw new CommonMessageException("Thông báo đã bị tắt");
        pushViaWebsocket(notification);
    }

    // ==================== SCHEDULER ====================

    /**
     * Chạy mỗi phút:
     * 1. Push notification đã đến giờ nhưng chưa push
     * 2. Deactivate notification đã hết hạn
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void ScheduledNotificationCheck() {
        Instant now = Instant.now();

        // 1. Push unpushed
        List<Notification> unpushed = notificationRepository.findUnpushedActiveNotifications(now);
        for (Notification n : unpushed) {
            pushViaWebsocket(n);
            log.info("⏰ Scheduled push: id={}, title='{}'", n.getId(), n.getTitle());
        }

        // 2. Deactivate expired
        int deactivated = notificationRepository.deactivateExpiredNotifications(now);
        if (deactivated > 0) log.info("🗑️ Deactivated {} expired notifications", deactivated);
    }


    // ==================== PRIVATE ====================
    private void pushViaWebsocket(Notification notification) {
        NotificationResponse response = convertToResponse(notification);
        messagingTemplate.convertAndSend("/topic/notifications", response);

        notification.setPushed(true);
        notificationRepository.save(notification);

        log.info("📡 WebSocket push: id={}, title='{}', type={}",
                notification.getId(), notification.getTitle(), notification.getType());
    }

    private boolean isCurrentlyActive(Notification n) {
        Instant now = Instant.now();
        return n.isActive() && !n.getStartAt().isAfter(now) && (n.getEndAt() == null || !n.getEndAt().isBefore(now));
    }

    private Notification findById(Long id) {
        return notificationRepository.findById(id).orElseThrow(() -> new CommonMessageException("Không tìm thấy thông báo"));
    }

    private NotificationResponse convertToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .target(n.getTarget())
                .imageUrl(n.getImageUrl())
                .actionUrl(n.getActionUrl())
                .actionText(n.getActionText())
                .severity(n.getSeverity())
                .startAt(n.getStartAt())
                .endAt(n.getEndAt())
                .active(n.isActive())
                .pushed(n.isPushed())
                .createdByName(n.getCreatedBy() != null ? n.getCreatedBy().getUsername() : "System")
                .createdAt(n.getCreatedAt())
                .build();
    }
}
