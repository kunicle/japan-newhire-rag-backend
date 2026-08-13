package com.teamproject.japan_newhire_rag_backend.rag.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.document.access.service.DocumentSearchScopeService;
import com.teamproject.japan_newhire_rag_backend.rag.model.EmbeddingModelSelection;
import com.teamproject.japan_newhire_rag_backend.rag.model.service.EmbeddingModelSelectionService;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.RagOrchestrationResult;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.RagOrchestrator;

@ExtendWith(MockitoExtension.class)
class RagQueryServiceTest {

    @Mock
    private DocumentSearchScopeService documentSearchScopeService;

    @Mock
    private EmbeddingModelSelectionService embeddingModelSelectionService;

    @Mock
    private RagOrchestrator ragOrchestrator;

    private RagQueryService service;

    @BeforeEach
    void setUp() {
        service = new RagQueryService(
                documentSearchScopeService,
                embeddingModelSelectionService,
                ragOrchestrator);
    }

    @Test
    void returnsInsufficientEvidenceWithoutCallingOrchestratorForEmptyScope() {
        when(documentSearchScopeService.findAllowedDocumentVersionIds()).thenReturn(Set.of());

        RagOrchestrationResult result = service.handle("취업 규칙을 알려주세요");

        assertFalse(result.hasSufficientEvidence());
        assertNull(result.answer());
        assertEquals(List.of(), result.validCitedChunkIds());
        verifyNoInteractions(embeddingModelSelectionService, ragOrchestrator);
    }

    @Test
    void passesSameAllowedScopeToOrchestrator() {
        Set<Long> allowedDocumentVersionIds = Set.of(10L, 20L);
        RagOrchestrationResult expected = new RagOrchestrationResult(false, null, List.of());
        when(documentSearchScopeService.findAllowedDocumentVersionIds())
                .thenReturn(allowedDocumentVersionIds);
        when(embeddingModelSelectionService.selectDefaultEmbeddingModel())
                .thenReturn(modelSelection());
        when(ragOrchestrator.handle(
                "질문", allowedDocumentVersionIds, "provider-a", "model-a"))
                .thenReturn(expected);

        service.handle("질문");

        verify(ragOrchestrator).handle(
                eq("질문"),
                same(allowedDocumentVersionIds),
                eq("provider-a"),
                eq("model-a"));
    }

    @Test
    void returnsExactResultFromOrchestrator() {
        Set<Long> allowedDocumentVersionIds = Set.of(10L);
        RagOrchestrationResult expected =
                new RagOrchestrationResult(true, "답변", List.of(100L));
        when(documentSearchScopeService.findAllowedDocumentVersionIds())
                .thenReturn(allowedDocumentVersionIds);
        when(embeddingModelSelectionService.selectDefaultEmbeddingModel())
                .thenReturn(modelSelection());
        when(ragOrchestrator.handle(
                "질문", allowedDocumentVersionIds, "provider-a", "model-a"))
                .thenReturn(expected);

        RagOrchestrationResult actual = service.handle("질문");

        assertSame(expected, actual);
    }

    @Test
    void propagatesDocumentSearchScopeServiceExceptionWithoutCallingOrchestrator() {
        IllegalStateException expected = new IllegalStateException("scope lookup failed");
        when(documentSearchScopeService.findAllowedDocumentVersionIds()).thenThrow(expected);

        IllegalStateException actual =
                assertThrows(IllegalStateException.class, () -> service.handle("질문"));

        assertSame(expected, actual);
        verifyNoInteractions(embeddingModelSelectionService, ragOrchestrator);
    }

    @Test
    void propagatesRagOrchestratorException() {
        Set<Long> allowedDocumentVersionIds = Set.of(10L);
        IllegalStateException expected = new IllegalStateException("orchestration failed");
        when(documentSearchScopeService.findAllowedDocumentVersionIds())
                .thenReturn(allowedDocumentVersionIds);
        when(embeddingModelSelectionService.selectDefaultEmbeddingModel())
                .thenReturn(modelSelection());
        when(ragOrchestrator.handle(
                "질문", allowedDocumentVersionIds, "provider-a", "model-a"))
                .thenThrow(expected);

        IllegalStateException actual =
                assertThrows(IllegalStateException.class, () -> service.handle("질문"));

        assertSame(expected, actual);
    }

    @Test
    void rejectsNullQuestionEvenWhenScopeIsEmpty() {
        lenient().when(documentSearchScopeService.findAllowedDocumentVersionIds()).thenReturn(Set.of());

        assertThrows(IllegalArgumentException.class, () -> service.handle(null));

        verifyNoInteractions(embeddingModelSelectionService, ragOrchestrator);
    }

    @Test
    void rejectsBlankQuestionEvenWhenScopeIsEmpty() {
        lenient().when(documentSearchScopeService.findAllowedDocumentVersionIds()).thenReturn(Set.of());

        assertThrows(IllegalArgumentException.class, () -> service.handle("   "));

        verifyNoInteractions(embeddingModelSelectionService, ragOrchestrator);
    }

    @Test
    void rejectsInvalidQuestionBeforeLookingUpNonEmptyScope() {
        lenient().when(documentSearchScopeService.findAllowedDocumentVersionIds())
                .thenReturn(Set.of(10L));

        assertThrows(IllegalArgumentException.class, () -> service.handle(null));
        assertThrows(IllegalArgumentException.class, () -> service.handle("   "));

        verifyNoInteractions(
                documentSearchScopeService,
                embeddingModelSelectionService,
                ragOrchestrator);
    }

    private EmbeddingModelSelection modelSelection() {
        return new EmbeddingModelSelection(1L, "provider-a", "model-a", 1536);
    }
}
