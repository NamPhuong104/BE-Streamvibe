package movieapp.repository;

import movieapp.entity.Notification;
import movieapp.util.constant.NotificationTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    /**
     * Thông báo đang active trong khoảng thời gian hiện tại
     * Dùng cho: GET /notifications/active
     */
    @Query("""
            SELECT n FROM Notification n
            WHERE n.active = true
            AND n.startAt <= :now
            AND (n.endAt IS NULL OR n.endAt >= :now)
            ORDER BY n.createdAt DESC
            """)
    List<Notification> findActiveNotifications(@Param("now") Instant now);

    // dùng cho public endpoint, filter theo target
    @Query("""
            SELECT n FROM Notification n
            WHERE n.active = true
            AND n.startAt <= :now
            AND (n.endAt IS NULL OR n.endAt >= :now)
            AND n.target IN :targets
            ORDER BY n.createdAt DESC
            """)
    List<Notification> findActiveByTargets(@Param("now") Instant now, @Param("targets") List<NotificationTarget> targets);

    /**
     * Thông báo chưa push nhưng đã đến giờ active
     * Dùng cho: @Scheduled task
     */
    @Query("""
            SELECT n FROM Notification n
            WHERE n.active = true
            AND n.pushed = false
            AND n.startAt <= :now
            AND (n.endAt IS NULL OR n.endAt >= :now)
            """)
    List<Notification> findUnpushedActiveNotifications(@Param("now") Instant now);

    /**
     * Tự động deactivate thông báo đã hết hạn
     */
    @Modifying
    @Query("""
            UPDATE Notification n SET n.active = false, n.updatedAt = :now
            WHERE n.active = true
            AND n.endAt IS NOT NULL
            AND n.endAt < :now
            """)
    int deactivateExpiredNotifications(@Param("now") Instant now);


    List<Notification> findAllByOrderByCreatedAtDesc();
}
