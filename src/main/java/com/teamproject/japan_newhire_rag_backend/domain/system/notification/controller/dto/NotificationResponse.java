package com.teamproject.japan_newhire_rag_backend.domain.system.notification.controller.dto;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.domain.system.notification.entity.Notification;

public record NotificationResponse(
        Long notificationId,
        String notificationType,
        String title,
        String message,
        String targetType,
        Long targetId,
        boolean isRead,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(), notification.getNotificationType(),
                notification.getTitle(), notification.getMessage(), notification.getTargetType(),
                notification.getTargetId(), notification.isRead(), notification.getReadAt(),
                notification.getCreatedAt());
    }
}
