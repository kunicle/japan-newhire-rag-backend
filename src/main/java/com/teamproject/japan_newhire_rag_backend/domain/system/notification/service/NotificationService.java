package com.teamproject.japan_newhire_rag_backend.domain.system.notification.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.controller.dto.NotificationPageResponse;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.controller.dto.NotificationResponse;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.entity.Notification;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.error.NotificationErrorCode;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.repository.NotificationRepository;

@Service
public class NotificationService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort LATEST_FIRST = Sort.by(
            Sort.Order.desc("createdAt"), Sort.Order.desc("notificationId"));

    private final NotificationRepository repository;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public NotificationService(
            NotificationRepository repository, CurrentUserProvider currentUserProvider, Clock clock
    ) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse findMine(Boolean read, int page, int size) {
        validatePage(page, size);
        Long appUserId = currentUserProvider.getCurrentUser().appUserId();
        PageRequest pageable = PageRequest.of(page, size, LATEST_FIRST);
        Page<Notification> notifications = read == null
                ? repository.findAllByRecipientAppUserId(appUserId, pageable)
                : repository.findAllByRecipientAppUserIdAndRead(appUserId, read, pageable);
        return NotificationPageResponse.from(notifications.map(NotificationResponse::from));
    }

    @Transactional
    public NotificationResponse markMineAsRead(Long notificationId) {
        Long appUserId = currentUserProvider.getCurrentUser().appUserId();
        Notification notification = repository
                .findByNotificationIdAndRecipientAppUserId(notificationId, appUserId)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
        notification.markAsRead(LocalDateTime.now(clock));
        return NotificationResponse.from(notification);
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be at least 0");
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }
}
