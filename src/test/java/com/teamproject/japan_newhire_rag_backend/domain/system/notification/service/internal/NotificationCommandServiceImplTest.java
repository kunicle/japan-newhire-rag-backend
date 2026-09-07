package com.teamproject.japan_newhire_rag_backend.domain.system.notification.service.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(saved.capture());
        Notification notification = saved.getValue();
        assertSame(recipient, notification.getRecipient());
        assertEquals("COURSE_ASSIGNED", notification.getNotificationType());
        assertEquals("New course", notification.getTitle());
        assertEquals("A course was assigned", notification.getMessage());
        assertFalse(notification.isRead());
        assertNull(notification.getReadAt());
        assertEquals(31L, notification.getTargetId());
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
