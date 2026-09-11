package com.teamproject.japan_newhire_rag_backend.domain.system.error.service.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.teamproject.japan_newhire_rag_backend.domain.system.error.api.SystemErrorRecordCommand;
import com.teamproject.japan_newhire_rag_backend.domain.system.error.entity.SystemErrorLog;
import com.teamproject.japan_newhire_rag_backend.domain.system.error.repository.SystemErrorLogRepository;

class SystemErrorRecordServiceImplTest {

    private SystemErrorLogRepository repository;
    private SystemErrorRecordServiceImpl service;

    @BeforeEach
    void setUp() { repository = mock(SystemErrorLogRepository.class); service = new SystemErrorRecordServiceImpl(repository); }

    @Test
    void recordsOpenErrorWithOptionalReferencesAndBoundaryRetryCounts() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 9, 11, 9, 0);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.record(new SystemErrorRecordCommand(null, null, "LLM_API", "HTTP_5XX", "503", 0, "HttpServerErrorException", occurredAt));
        service.record(new SystemErrorRecordCommand(1L, 2L, "EMBEDDING_API", "RATE_LIMIT", "429", 2, "TooManyRequests", occurredAt));
        ArgumentCaptor<SystemErrorLog> captor = ArgumentCaptor.forClass(SystemErrorLog.class);
        verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());
        SystemErrorLog first = captor.getAllValues().get(0);
        SystemErrorLog second = captor.getAllValues().get(1);
        assertNull(first.getAppUserId()); assertNull(first.getExternalApiCallLogId());
        assertEquals("OPEN", first.getErrorStatus()); assertEquals(occurredAt, first.getOccurredAt());
        assertEquals(2, second.getRetryCount()); assertEquals("TooManyRequests", second.getErrorMessage());
    }

    @Test
    void rejectsRetryCountOutsideDocumentedRange() {
        assertThrows(IllegalArgumentException.class, () -> service.record(command(-1)));
        assertThrows(IllegalArgumentException.class, () -> service.record(command(3)));
    }

    private SystemErrorRecordCommand command(int retryCount) {
        return new SystemErrorRecordCommand(null, null, "LLM_API", "NETWORK_ERROR", null,
                retryCount, "ResourceAccessException", LocalDateTime.of(2026, 9, 11, 9, 0));
    }
}
