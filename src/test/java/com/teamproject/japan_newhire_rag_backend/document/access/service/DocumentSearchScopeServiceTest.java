package com.teamproject.japan_newhire_rag_backend.document.access.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.document.version.service.DocumentVersionCandidateService;

@ExtendWith(MockitoExtension.class)
class DocumentSearchScopeServiceTest {

    @Mock
    private DocumentVersionCandidateService documentVersionCandidateService;

    @Mock
    private DocumentAccessScopeService documentAccessScopeService;

    private DocumentSearchScopeService service;

    @BeforeEach
    void setUp() {
        service = new DocumentSearchScopeService(
                documentVersionCandidateService, documentAccessScopeService);
    }

    @Test
    void returnsEmptySetWithoutCallingAccessScopeWhenNoCandidates() {
        when(documentVersionCandidateService.findCandidateDocumentVersionIds()).thenReturn(Set.of());

        Set<Long> result = service.findAllowedDocumentVersionIds();

        assertEquals(Set.of(), result);
        verifyNoInteractions(documentAccessScopeService);
    }

    @Test
    void passesCandidateIdsToAccessScopeService() {
        Set<Long> candidates = Set.of(1L, 2L, 3L);
        when(documentVersionCandidateService.findCandidateDocumentVersionIds()).thenReturn(candidates);
        when(documentAccessScopeService.filterAccessibleDocumentVersionIds(candidates))
                .thenReturn(Set.of(1L, 3L));

        service.findAllowedDocumentVersionIds();

        verify(documentAccessScopeService).filterAccessibleDocumentVersionIds(candidates);
    }

    @Test
    void returnsPartiallyFilteredAccessScopeResultAsIs() {
        Set<Long> candidates = Set.of(1L, 2L, 3L);
        when(documentVersionCandidateService.findCandidateDocumentVersionIds()).thenReturn(candidates);
        when(documentAccessScopeService.filterAccessibleDocumentVersionIds(candidates))
                .thenReturn(Set.of(1L, 3L));

        Set<Long> result = service.findAllowedDocumentVersionIds();

        assertEquals(Set.of(1L, 3L), result);
    }

    @Test
    void propagatesExceptionFromCandidateServiceWithoutCallingAccessScope() {
        when(documentVersionCandidateService.findCandidateDocumentVersionIds())
                .thenThrow(new IllegalStateException("candidate lookup failed"));

        assertThrows(IllegalStateException.class, () -> service.findAllowedDocumentVersionIds());

        verifyNoInteractions(documentAccessScopeService);
    }

    @Test
    void propagatesExceptionFromAccessScopeService() {
        Set<Long> candidates = Set.of(1L);
        when(documentVersionCandidateService.findCandidateDocumentVersionIds()).thenReturn(candidates);
        when(documentAccessScopeService.filterAccessibleDocumentVersionIds(candidates))
                .thenThrow(new IllegalStateException("access scope failed"));

        assertThrows(IllegalStateException.class, () -> service.findAllowedDocumentVersionIds());
    }
}
