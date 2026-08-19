package com.teamproject.japan_newhire_rag_backend.domain.system.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.teamproject.japan_newhire_rag_backend.domain.system.audit.entity.AuditLog;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditActionType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.enums.AuditTargetType;
import com.teamproject.japan_newhire_rag_backend.domain.system.audit.repository.AuditLogRepository;

class AuditLogQueryServiceTest {

    private AuditLogRepository repository;
    private AuditLogQueryService service;

    @BeforeEach
    void setUp() {
        repository = mock(AuditLogRepository.class);
        service = new AuditLogQueryService(repository);
    }

    @Test
    void appliesFiltersPaginationAndStableLatestFirstSort() {
        when(repository.findAllByFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.findAll(
                AuditActionType.ROLE_GRANTED,
                113L,
                AuditTargetType.APP_USER,
                123L,
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 8, 31, 23, 59, 59),
                2,
                30);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllByFilters(any(), any(), any(), any(), any(), any(), pageable.capture());
        assertEquals(2, pageable.getValue().getPageNumber());
        assertEquals(30, pageable.getValue().getPageSize());
        assertEquals("createdAt: DESC,auditLogId: DESC", pageable.getValue().getSort().toString());
    }

    @Test
    void returnsMappedPageMetadata() {
        AuditLog log = AuditLog.record(
                1L, AuditActionType.USER_CREATED, AuditTargetType.APP_USER, 2L,
                null, "{}", "127.0.0.1", "request-1",
                LocalDateTime.of(2026, 8, 19, 12, 0));
        when(repository.findAllByFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(log)));

        var response = service.findAll(null, null, null, null, null, null, 0, 20);

        assertEquals(1, response.content().size());
        assertEquals("request-1", response.content().get(0).requestId());
        assertEquals(1, response.totalElements());
    }

    @Test
    void rejectsInvalidPageSizeAndDateRange() {
        assertThrows(IllegalArgumentException.class,
                () -> service.findAll(null, null, null, null, null, null, -1, 20));
        assertThrows(IllegalArgumentException.class,
                () -> service.findAll(null, null, null, null, null, null, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> service.findAll(null, null, null, null, null, null, 0, 101));
        assertThrows(IllegalArgumentException.class,
                () -> service.findAll(null, null, null, null,
                        LocalDateTime.of(2026, 8, 2, 0, 0),
                        LocalDateTime.of(2026, 8, 1, 0, 0), 0, 20));
    }
}
