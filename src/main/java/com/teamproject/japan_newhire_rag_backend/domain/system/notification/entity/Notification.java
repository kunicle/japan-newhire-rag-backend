package com.teamproject.japan_newhire_rag_backend.domain.system.notification.entity;

import java.time.LocalDateTime;

import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser recipient;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "notification_title", nullable = false, length = 200)
    private String title;

    @Column(name = "notification_content", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "reference_type", length = 50)
    private String targetType;

    @Column(name = "reference_id")
    private Long targetId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Notification create(
            AppUser recipient, String notificationType, String title, String message,
            String targetType, Long targetId, LocalDateTime createdAt
    ) {
        Notification notification = new Notification();
        notification.recipient = recipient;
        notification.notificationType = notificationType;
        notification.title = title;
        notification.message = message;
        notification.targetType = targetType;
        notification.targetId = targetId;
        notification.createdAt = createdAt;
        return notification;
    }

    public void markAsRead(LocalDateTime now) {
        if (!read) {
            read = true;
            readAt = now;
        }
    }
}
