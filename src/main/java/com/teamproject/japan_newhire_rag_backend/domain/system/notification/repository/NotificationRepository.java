package com.teamproject.japan_newhire_rag_backend.domain.system.notification.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teamproject.japan_newhire_rag_backend.domain.system.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByRecipientAppUserId(Long appUserId, Pageable pageable);

    Page<Notification> findAllByRecipientAppUserIdAndRead(
            Long appUserId, boolean read, Pageable pageable);

    Optional<Notification> findByNotificationIdAndRecipientAppUserId(
            Long notificationId, Long appUserId);

    @Modifying
    @Query(value = """
            INSERT INTO notification (
                app_user_id, notification_type, notification_title,
                notification_content, reference_type, reference_id,
                is_read, read_at, created_at
            ) VALUES (
                :recipientAppUserId, :notificationType, :title,
                :message, :targetType, :targetId,
                FALSE, NULL, :createdAt
            ) ON DUPLICATE KEY UPDATE notification_id = notification_id
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("recipientAppUserId") Long recipientAppUserId,
            @Param("notificationType") String notificationType,
            @Param("title") String title,
            @Param("message") String message,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("createdAt") java.time.LocalDateTime createdAt);
}
