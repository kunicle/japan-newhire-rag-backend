package com.teamproject.japan_newhire_rag_backend.rag.persistence.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.ExternalApiCallLog;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.repository.ExternalApiCallLogRepository;

class ExternalApiCallLogServiceTest {

    private static final LocalDateTime REQUESTED_AT = LocalDateTime.of(2026, 9, 14, 10, 0);

    private final ExternalApiCallLogRepository repository = mock(ExternalApiCallLogRepository.class);
    private final ExternalApiCallLogService service = new ExternalApiCallLogService(repository);

    @Test
    void rejectsCommandWithNeitherRagQuestionIdNorDocumentProcessingJobId() {
        ExternalApiCallLogCommand command = commandWith(null, null);

        assertThrows(IllegalArgumentException.class, () -> service.record(command));
    }

    @Test
    void allowsCommandWithOnlyRagQuestionId() {
        ExternalApiCallLog saved = mock(ExternalApiCallLog.class);
        when(saved.getExternalApiCallLogId()).thenReturn(501L);
        when(repository.save(any(ExternalApiCallLog.class))).thenReturn(saved);

        Long id = service.record(commandWith(101L, null));

        assertEquals(501L, id);
    }

    @Test
    void allowsCommandWithOnlyDocumentProcessingJobId() {
        ExternalApiCallLog saved = mock(ExternalApiCallLog.class);
        when(saved.getExternalApiCallLogId()).thenReturn(502L);
        when(repository.save(any(ExternalApiCallLog.class))).thenReturn(saved);

        Long id = service.record(commandWith(null, 201L));

        assertEquals(502L, id);
    }

    @Test
    void allowsCommandWithBothRagQuestionIdAndDocumentProcessingJobIdPresent() {
        // No current caller populates both fields at once, and no validation or
        // documented policy forbids it -- this preserves that existing (permissive)
        // behavior rather than introducing a new prohibition.
        ExternalApiCallLog saved = mock(ExternalApiCallLog.class);
        when(saved.getExternalApiCallLogId()).thenReturn(503L);
        when(repository.save(any(ExternalApiCallLog.class))).thenReturn(saved);

        Long id = service.record(commandWith(101L, 201L));

        assertEquals(503L, id);
    }

    private ExternalApiCallLogCommand commandWith(Long ragQuestionId, Long documentProcessingJobId) {
        return new ExternalApiCallLogCommand(
                10L, ragQuestionId, documentProcessingJobId, "RAG_SEARCH", "COMPLETED",
                1, 200, null, null, 120, REQUESTED_AT, REQUESTED_AT);
    }
}
