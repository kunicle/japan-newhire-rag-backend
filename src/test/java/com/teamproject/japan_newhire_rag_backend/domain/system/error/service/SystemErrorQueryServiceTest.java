package com.teamproject.japan_newhire_rag_backend.domain.system.error.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.teamproject.japan_newhire_rag_backend.domain.system.error.entity.SystemErrorLog;
import com.teamproject.japan_newhire_rag_backend.domain.system.error.repository.SystemErrorLogRepository;

class SystemErrorQueryServiceTest {
    private SystemErrorLogRepository repository;
    private SystemErrorQueryService service;
    @BeforeEach void setUp() { repository = mock(SystemErrorLogRepository.class); service = new SystemErrorQueryService(repository); }

    @Test
    void usesDefaultCompatiblePaginationAndLatestFirstSort() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(error())));
        var response = service.findAll(0, 20);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(captor.capture());
        assertEquals(0, captor.getValue().getPageNumber()); assertEquals(20, captor.getValue().getPageSize());
        assertEquals("occurredAt: DESC,systemErrorLogId: DESC", captor.getValue().getSort().toString());
        assertEquals(1, response.content().size());
    }

    @Test
    void acceptsMaxSizeAndRejectsInvalidPagination() {
        when(repository.findAll(any(Pageable.class))).thenReturn(org.springframework.data.domain.Page.empty());
        service.findAll(1, 100);
        assertThrows(IllegalArgumentException.class, () -> service.findAll(-1, 20));
        assertThrows(IllegalArgumentException.class, () -> service.findAll(0, 0));
        assertThrows(IllegalArgumentException.class, () -> service.findAll(0, 101));
    }

    private SystemErrorLog error() { return SystemErrorLog.open(null, null, "LLM_API", "HTTP_5XX", "503", 2, "HttpServerErrorException", LocalDateTime.of(2026, 9, 11, 9, 0)); }
}
