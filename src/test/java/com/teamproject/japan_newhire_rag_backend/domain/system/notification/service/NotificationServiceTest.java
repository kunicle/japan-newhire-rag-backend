package com.teamproject.japan_newhire_rag_backend.domain.system.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.domain.auth.entity.AppUser;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.entity.Notification;
import com.teamproject.japan_newhire_rag_backend.domain.system.notification.repository.NotificationRepository;

class NotificationServiceTest {

    private NotificationRepository repository;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationRepository.class);
        CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
        when(currentUser.getCurrentUser()).thenReturn(
                new CurrentUserContext(10L, 20L, Set.of(), 30L, 1, null));
        service = new NotificationService(repository, currentUser,
                Clock.fixed(Instant.parse("2026-08-19T03:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void scopesListToCurrentUserAndAppliesStableSortAndFilter() {
        when(repository.findAllByRecipientAppUserIdAndRead(any(), any(Boolean.class), any()))
                .thenReturn(Page.empty());

        service.findMine(false, 2, 30);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllByRecipientAppUserIdAndRead(
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(false), pageable.capture());
        assertEquals(2, pageable.getValue().getPageNumber());
        assertEquals(30, pageable.getValue().getPageSize());
        assertEquals("createdAt: DESC,notificationId: DESC", pageable.getValue().getSort().toString());
    }

    @Test
    void validatesPagination() {
        assertThrows(IllegalArgumentException.class, () -> service.findMine(null, -1, 20));
        assertThrows(IllegalArgumentException.class, () -> service.findMine(null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> service.findMine(null, 0, 101));
    }

    @Test
    void marksOwnedNotificationReadIdempotentlyAndPreservesReadAt() {
        Notification notification = Notification.create(
                AppUser.createActive("user@example.com", "hash"), "GENERAL", "Title", "Message",
                null, null, LocalDateTime.of(2026, 8, 18, 12, 0));
        when(repository.findByNotificationIdAndRecipientAppUserId(5L, 10L))
                .thenReturn(Optional.of(notification));

        service.markMineAsRead(5L);
        LocalDateTime firstReadAt = notification.getReadAt();
        service.markMineAsRead(5L);

        assertNotNull(firstReadAt);
        assertEquals(firstReadAt, notification.getReadAt());
    }

    @Test
    void treatsMissingAndOtherUsersNotificationAsNotFound() {
        when(repository.findByNotificationIdAndRecipientAppUserId(5L, 10L))
                .thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.markMineAsRead(5L));
    }
}
