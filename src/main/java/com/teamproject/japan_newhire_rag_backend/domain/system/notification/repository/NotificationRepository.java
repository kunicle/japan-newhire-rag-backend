package com.teamproject.japan_newhire_rag_backend.domain.system.notification.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.teamproject.japan_newhire_rag_backend.domain.system.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByRecipientAppUserId(Long appUserId, Pageable pageable);

    Page<Notification> findAllByRecipientAppUserIdAndRead(
            Long appUserId, boolean read, Pageable pageable);

    Optional<Notification> findByNotificationIdAndRecipientAppUserId(
            Long notificationId, Long appUserId);
}
