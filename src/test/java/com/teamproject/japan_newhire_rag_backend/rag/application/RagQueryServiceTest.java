package com.teamproject.japan_newhire_rag_backend.rag.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.document.access.service.DocumentSearchScopeService;
import com.teamproject.japan_newhire_rag_backend.rag.model.EmbeddingModelSelection;
import com.teamproject.japan_newhire_rag_backend.rag.model.service.EmbeddingModelSelectionService;

@ExtendWith(MockitoExtension.class)
class RagQueryServiceTest {

    @Mock
    private DocumentSearchScopeService documentSearchScopeService;

    @Mock
    private EmbeddingModelSelectionService embeddingModelSelectionService;

    private RagQueryService service;

    @BeforeEach
    void setUp() {
        service = new RagQueryService(
                documentSearchScopeService,
                embeddingModelSelectionService);
    }

    @Test
    void rejectsNullQuestionEvenWhenScopeIsEmpty() {
        lenient().when(documentSearchScopeService.findAllowedDocumentVersionIds()).thenReturn(Set.of());

        assertThrows(IllegalArgumentException.class, () -> service.prepareSearch(null));

        verifyNoInteractions(documentSearchScopeService, embeddingModelSelectionService);
    }

    @Test
    void rejectsBlankQuestionEvenWhenScopeIsEmpty() {
        lenient().when(documentSearchScopeService.findAllowedDocumentVersionIds()).thenReturn(Set.of());

        assertThrows(IllegalArgumentException.class, () -> service.prepareSearch("   "));

        verifyNoInteractions(documentSearchScopeService, embeddingModelSelectionService);
    }

    @Test
    void rejectsInvalidQuestionBeforeLookingUpNonEmptyScope() {
        lenient().when(documentSearchScopeService.findAllowedDocumentVersionIds())
                .thenReturn(Set.of(10L));

        assertThrows(IllegalArgumentException.class, () -> service.prepareSearch(null));
        assertThrows(IllegalArgumentException.class, () -> service.prepareSearch("   "));

        verifyNoInteractions(documentSearchScopeService, embeddingModelSelectionService);
    }

    @Test
    void returnsEmptyOptionalWithoutSelectingModelForEmptyScope() {
        when(documentSearchScopeService.findAllowedDocumentVersionIds()).thenReturn(Set.of());

        Optional<RagSearchPlan> result = service.prepareSearch("질문");

        assertTrue(result.isEmpty());
        verifyNoInteractions(embeddingModelSelectionService);
    }

    @Test
    void returnsPlanWithExactModelSelectionAndScope() {
        Set<Long> allowedDocumentVersionIds = Set.of(10L, 20L);
        EmbeddingModelSelection modelSelection = modelSelection();
        when(documentSearchScopeService.findAllowedDocumentVersionIds())
                .thenReturn(allowedDocumentVersionIds);
        when(embeddingModelSelectionService.selectDefaultEmbeddingModel())
                .thenReturn(modelSelection);

        RagSearchPlan plan = service.prepareSearch("질문").orElseThrow();

        assertEquals(modelSelection.aiModelId(), plan.aiModelId());
        assertEquals(allowedDocumentVersionIds, plan.allowedDocumentVersionIds());
        assertEquals(modelSelection.providerName(), plan.providerName());
        assertEquals(modelSelection.modelName(), plan.modelName());
    }

    @Test
    void propagatesDocumentSearchScopeServiceExceptionWithoutSelectingModel() {
        IllegalStateException expected = new IllegalStateException("scope lookup failed");
        when(documentSearchScopeService.findAllowedDocumentVersionIds()).thenThrow(expected);

        IllegalStateException actual = assertThrows(
                IllegalStateException.class,
                () -> service.prepareSearch("질문"));

        assertSame(expected, actual);
        verifyNoInteractions(embeddingModelSelectionService);
    }

    @Test
    void propagatesEmbeddingModelSelectionServiceException() {
        IllegalStateException expected = new IllegalStateException("model lookup failed");
        when(documentSearchScopeService.findAllowedDocumentVersionIds())
                .thenReturn(Set.of(10L));
        when(embeddingModelSelectionService.selectDefaultEmbeddingModel()).thenThrow(expected);

        IllegalStateException actual = assertThrows(
                IllegalStateException.class,
                () -> service.prepareSearch("질문"));

        assertSame(expected, actual);
    }

    private EmbeddingModelSelection modelSelection() {
        return new EmbeddingModelSelection(1L, "provider-a", "model-a", 1536);
    }
}
