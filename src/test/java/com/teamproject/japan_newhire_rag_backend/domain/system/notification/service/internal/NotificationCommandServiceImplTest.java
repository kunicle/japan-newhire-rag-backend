package com.teamproject.japan_newhire_rag_backend.domain.system.notification.service.internal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.auth.repository.AppUserRepository;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.api.NotificationSendCommand;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.entity.Notification;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.repository.NotificationRepository;

class NotificationCommandServiceImplTest {

    private NotificationRepository notifications;
    private AppUserRepository users;
    private NotificationCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        notifications = mock(NotificationRepository.class);
        users = mock(AppUserRepository.class);
        service = new NotificationCommandServiceImpl(notifications, users,
                Clock.fixed(Instant.parse("2026-08-19T03:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void sendsUnreadNotificationToExistingRecipient() {
        AppUser recipient = AppUser.createActive("user@example.com", "hash");
        when(users.findById(7L)).thenReturn(Optional.of(recipient));

        service.send(new NotificationSendCommand(
                7L, "COURSE_ASSIGNED", "New course", "A course was assigned",
                "COURSE", 31L));

        verify(notifications).insertIfAbsent(
                7L, "COURSE_ASSIGNED", "New course", "A course was assigned",
                "COURSE", 31L, java.time.LocalDateTime.of(2026, 8, 19, 3, 0));
    }

    @Test
    void delegatesRepeatedEventSendsToDatabaseIdempotency() {
        AppUser recipient = AppUser.createActive("user@example.com", "hash");
        when(users.findById(7L)).thenReturn(Optional.of(recipient));
        NotificationSendCommand command = new NotificationSendCommand(
                7L, "EVALUATION_STARTED", "Started", "Evaluation started",
                "EVALUATION_CYCLE", 4L);

        service.send(command);
        service.send(command);

        verify(notifications, times(2)).insertIfAbsent(
                7L, "EVALUATION_STARTED", "Started", "Evaluation started",
                "EVALUATION_CYCLE", 4L,
                java.time.LocalDateTime.of(2026, 8, 19, 3, 0));
    }

    @Test
    void rejectsMissingRecipientAndInvalidRequiredInput() {
        when(users.findById(999L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.send(new NotificationSendCommand(
                999L, "GENERAL", "Title", "Message", null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> new NotificationSendCommand(null, "GENERAL", "Title", "Message", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new NotificationSendCommand(1L, " ", "Title", "Message", null, null));
        assertThrows(IllegalArgumentException.class, () -> service.send(null));
    }
}
