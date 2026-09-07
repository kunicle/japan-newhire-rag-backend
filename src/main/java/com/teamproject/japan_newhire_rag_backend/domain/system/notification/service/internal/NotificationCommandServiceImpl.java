package com.teamproject.japan_newhire_rag_backend.domain.system.notification.service.internal;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.AppUserRepository;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.api.NotificationCommandService;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.api.NotificationSendCommand;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.entity.Notification;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.error.NotificationErrorCode;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.repository.NotificationRepository;

@Service
@Transactional
public class NotificationCommandServiceImpl implements NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final AppUserRepository appUserRepository;
    private final Clock clock;

    public NotificationCommandServiceImpl(
            NotificationRepository notificationRepository,
            AppUserRepository appUserRepository,
            Clock clock
    ) {
        this.notificationRepository = notificationRepository;
        this.appUserRepository = appUserRepository;
        this.clock = clock;
    }

    @Override
    public void send(NotificationSendCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command is required");
        }
        AppUser recipient = appUserRepository.findById(command.recipientAppUserId())
                .filter(user -> user.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.RECIPIENT_NOT_FOUND));
        notificationRepository.save(Notification.create(
                recipient, command.notificationType(), command.title(), command.message(),
                command.targetType(), command.targetId(), LocalDateTime.now(clock)));
    }
}
