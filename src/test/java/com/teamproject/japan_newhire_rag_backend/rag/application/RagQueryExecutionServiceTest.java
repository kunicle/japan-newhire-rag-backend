package com.teamproject.japan_newhire_rag_backend.rag.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserProvider;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagSearchResultItem;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.RagGenerationOrchestrationResult;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.ExternalAiCallException;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.RagOrchestrator;
import com.teamproject.japan_newhire_rag_backend.rag.orchestration.RagSearchOrchestrationResult;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagQuestion;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagSearch;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.service.RagPersistenceService;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.service.RagSearchPersistenceItem;

@ExtendWith(MockitoExtension.class)
class RagQueryExecutionServiceTest {

    private static final String QUESTION = "휴가 규정";

    @Mock
    private RagQueryService ragQueryService;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private RagPersistenceService ragPersistenceService;
    @Mock
    private RagOrchestrator ragOrchestrator;

    private RagQueryExecutionService service;

    @BeforeEach
    void setUp() {
        service = new RagQueryExecutionService(
                ragQueryService,
                currentUserProvider,
                ragPersistenceService,
                ragOrchestrator);
    }

    @Test
    void propagatesPrepareSearchFailureWithoutFurtherInteractions() {
        IllegalArgumentException expected = new IllegalArgumentException("invalid question");
        when(ragQueryService.prepareSearch(QUESTION)).thenThrow(expected);

        IllegalArgumentException actual = assertThrows(
                IllegalArgumentException.class,
                () -> service.execute(QUESTION));

        assertSame(expected, actual);
        verifyNoInteractions(currentUserProvider, ragPersistenceService, ragOrchestrator);
    }

    @Test
    void propagatesCurrentUserFailureWithoutPersistenceOrOrchestration() {
        IllegalStateException expected = new IllegalStateException("current user unavailable");
        when(ragQueryService.prepareSearch(QUESTION)).thenReturn(Optional.of(plan()));
        when(currentUserProvider.getCurrentUser()).thenThrow(expected);

        IllegalStateException actual = assertThrows(
                IllegalStateException.class,
                () -> service.execute(QUESTION));

        assertSame(expected, actual);
        verifyNoInteractions(ragPersistenceService, ragOrchestrator);
    }

    @Test
    void persistsQuestionAndReturnsInsufficientForEmptyPlan() {
        RagQuestion ragQuestion = org.mockito.Mockito.mock(RagQuestion.class);
        when(ragQueryService.prepareSearch(QUESTION)).thenReturn(Optional.empty());
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser());
        when(ragPersistenceService.persistQuestion(QUESTION, 1001L)).thenReturn(ragQuestion);

        RagQueryResult result = service.execute(QUESTION);

        assertEquals(new RagQueryResult(false, null, List.of()), result);
        InOrder order = inOrder(ragPersistenceService);
        order.verify(ragPersistenceService).persistQuestion(QUESTION, 1001L);
        order.verify(ragPersistenceService).markQuestionRejected(
                ragQuestion,
                "NO_ACCESSIBLE_DOCUMENT",
                "접근 가능한 규정 문서가 없습니다.");
        verify(ragPersistenceService, never()).persistSearch(
                any(), any(), anyList());
        verify(ragPersistenceService, never()).persistAnswer(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList());
        verifyNoInteractions(ragOrchestrator);
    }

    @Test
    void propagatesPersistQuestionFailureBeforeSearch() {
        IllegalStateException expected = new IllegalStateException("question persistence failed");
        when(ragQueryService.prepareSearch(QUESTION)).thenReturn(Optional.of(plan()));
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser());
        when(ragPersistenceService.persistQuestion(QUESTION, 1001L)).thenThrow(expected);

        IllegalStateException actual = assertThrows(
                IllegalStateException.class,
                () -> service.execute(QUESTION));

        assertSame(expected, actual);
        verifyNoInteractions(ragOrchestrator);
        verify(ragPersistenceService, never()).persistSearch(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList());
        verify(ragPersistenceService, never()).persistAnswer(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void propagatesSearchFailureAfterQuestionPersistence() {
        RagQuestion ragQuestion = org.mockito.Mockito.mock(RagQuestion.class);
        IllegalStateException expected = new IllegalStateException("search failed");
        stubQuestionPersistence(ragQuestion);
        when(ragOrchestrator.search(QUESTION, Set.of(1L, 2L), "provider", "model"))
                .thenThrow(new ExternalAiCallException(expected));

        IllegalStateException actual = assertThrows(
                IllegalStateException.class,
                () -> service.execute(QUESTION));

        assertSame(expected, actual);
        InOrder order = inOrder(ragPersistenceService, ragOrchestrator);
        order.verify(ragPersistenceService).persistQuestion(QUESTION, 1001L);
        order.verify(ragPersistenceService).markQuestionProcessing(ragQuestion);
        order.verify(ragOrchestrator).search(
                QUESTION, Set.of(1L, 2L), "provider", "model");
        order.verify(ragPersistenceService).markQuestionFailed(
                ragQuestion,
                "API_ERROR",
                "외부 AI 서비스 호출에 실패했습니다.");
        verify(ragPersistenceService, never()).persistSearch(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList());
        verify(ragOrchestrator, never()).generate(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(ragPersistenceService, never()).persistAnswer(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void doesNotMarkApiErrorForInternalSearchFailure() {
        RagQuestion ragQuestion = org.mockito.Mockito.mock(RagQuestion.class);
        IllegalStateException expected = new IllegalStateException("verification failed");
        stubQuestionPersistence(ragQuestion);
        when(ragOrchestrator.search(QUESTION, Set.of(1L, 2L), "provider", "model"))
                .thenThrow(expected);

        IllegalStateException actual = assertThrows(
                IllegalStateException.class,
                () -> service.execute(QUESTION));

        assertSame(expected, actual);
        verify(ragPersistenceService, never()).markQuestionFailed(any(), any(), any());
        verify(ragPersistenceService, never()).persistSearch(any(), any(), anyList());
        verify(ragPersistenceService, never()).persistAnswer(any(), any(), anyList());
    }

    @Test
    void propagatesPersistSearchFailureWithoutGeneration() {
        RagQuestion ragQuestion = org.mockito.Mockito.mock(RagQuestion.class);
        RagSearchOrchestrationResult searchResult = sufficientSearchResult();
        IllegalStateException expected = new IllegalStateException("search persistence failed");
        stubQuestionPersistence(ragQuestion);
        when(ragOrchestrator.search(QUESTION, Set.of(1L, 2L), "provider", "model"))
                .thenReturn(searchResult);
        when(ragPersistenceService.persistSearch(same(ragQuestion), eq(10L), anyList()))
                .thenThrow(expected);

        IllegalStateException actual = assertThrows(
                IllegalStateException.class,
                () -> service.execute(QUESTION));

        assertSame(expected, actual);
        verify(ragPersistenceService).markQuestionProcessing(ragQuestion);
        verify(ragOrchestrator, never()).generate(any(), any());
        verify(ragPersistenceService, never()).persistAnswer(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void persistsEmptySearchResultsBeforeReturningInsufficient() {
        RagQuestion ragQuestion = org.mockito.Mockito.mock(RagQuestion.class);
        RagSearch ragSearch = org.mockito.Mockito.mock(RagSearch.class);
        RagSearchOrchestrationResult searchResult =
                new RagSearchOrchestrationResult(false, List.of());
        stubQuestionPersistence(ragQuestion);
        when(ragOrchestrator.search(QUESTION, Set.of(1L, 2L), "provider", "model"))
                .thenReturn(searchResult);
        when(ragPersistenceService.persistSearch(same(ragQuestion), eq(10L), anyList()))
                .thenReturn(ragSearch);

        RagQueryResult result = service.execute(QUESTION);

        ArgumentCaptor<List<RagSearchPersistenceItem>> captor = persistenceItemsCaptor();
        verify(ragPersistenceService).persistSearch(same(ragQuestion), eq(10L), captor.capture());
        assertEquals(List.of(), captor.getValue());
        assertEquals(new RagQueryResult(false, null, List.of()), result);
        InOrder order = inOrder(ragPersistenceService, ragOrchestrator);
        order.verify(ragPersistenceService).markQuestionProcessing(ragQuestion);
        order.verify(ragOrchestrator).search(
                QUESTION, Set.of(1L, 2L), "provider", "model");
        order.verify(ragPersistenceService).persistSearch(same(ragQuestion), eq(10L), anyList());
        order.verify(ragPersistenceService).markQuestionRejected(
                ragQuestion,
                "LOW_SIMILARITY",
                "답변을 생성할 충분한 근거를 찾지 못했습니다.");
        verify(ragOrchestrator, never()).generate(any(), any());
        verify(ragPersistenceService, never()).persistAnswer(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void mapsAndPersistsNonemptyResultsBeforeReturningInsufficient() {
        RagQuestion ragQuestion = org.mockito.Mockito.mock(RagQuestion.class);
        RagSearch ragSearch = org.mockito.Mockito.mock(RagSearch.class);
        List<AiRagSearchResultItem> verifiedResults = List.of(
                searchItem(101L, 201L, "first", 0.61),
                searchItem(102L, 202L, "second", 0.52));
        RagSearchOrchestrationResult searchResult =
                new RagSearchOrchestrationResult(false, verifiedResults);
        stubQuestionPersistence(ragQuestion);
        when(ragOrchestrator.search(QUESTION, Set.of(1L, 2L), "provider", "model"))
                .thenReturn(searchResult);
        when(ragPersistenceService.persistSearch(same(ragQuestion), eq(10L), anyList()))
                .thenReturn(ragSearch);

        RagQueryResult result = service.execute(QUESTION);

        ArgumentCaptor<List<RagSearchPersistenceItem>> captor = persistenceItemsCaptor();
        verify(ragPersistenceService).persistSearch(same(ragQuestion), eq(10L), captor.capture());
        assertEquals(List.of(
                new RagSearchPersistenceItem(201L, 101L, 0.61),
                new RagSearchPersistenceItem(202L, 102L, 0.52)), captor.getValue());
        assertEquals(new RagQueryResult(false, null, List.of()), result);
        InOrder order = inOrder(ragPersistenceService, ragOrchestrator);
        order.verify(ragPersistenceService).markQuestionProcessing(ragQuestion);
        order.verify(ragOrchestrator).search(
                QUESTION, Set.of(1L, 2L), "provider", "model");
        order.verify(ragPersistenceService).persistSearch(same(ragQuestion), eq(10L), anyList());
        order.verify(ragPersistenceService).markQuestionRejected(
                ragQuestion,
                "LOW_SIMILARITY",
                "답변을 생성할 충분한 근거를 찾지 못했습니다.");
        verify(ragOrchestrator, never()).generate(any(), any());
        verify(ragPersistenceService, never()).persistAnswer(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void propagatesGenerateFailureAfterPersistSearch() {
        RagQuestion ragQuestion = org.mockito.Mockito.mock(RagQuestion.class);
        RagSearch ragSearch = org.mockito.Mockito.mock(RagSearch.class);
        RagSearchOrchestrationResult searchResult = sufficientSearchResult();
        IllegalStateException expected = new IllegalStateException("generate failed");
        stubThroughPersistSearch(ragQuestion, ragSearch, searchResult);
        when(ragOrchestrator.generate(QUESTION, searchResult))
                .thenThrow(new ExternalAiCallException(expected));

        IllegalStateException actual = assertThrows(
                IllegalStateException.class,
                () -> service.execute(QUESTION));

        assertSame(expected, actual);
        InOrder order = inOrder(ragPersistenceService, ragOrchestrator);
        order.verify(ragPersistenceService).markQuestionProcessing(ragQuestion);
        order.verify(ragOrchestrator).search(
                QUESTION, Set.of(1L, 2L), "provider", "model");
        order.verify(ragPersistenceService).persistSearch(same(ragQuestion), eq(10L), anyList());
        order.verify(ragOrchestrator).generate(QUESTION, searchResult);
        order.verify(ragPersistenceService).markQuestionFailed(
                ragQuestion,
                "API_ERROR",
                "외부 AI 서비스 호출에 실패했습니다.");
        verify(ragPersistenceService, never()).persistAnswer(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void doesNotMarkApiErrorForInternalGenerateFailure() {
        RagQuestion ragQuestion = org.mockito.Mockito.mock(RagQuestion.class);
        RagSearch ragSearch = org.mockito.Mockito.mock(RagSearch.class);
        RagSearchOrchestrationResult searchResult = sufficientSearchResult();
        IllegalStateException expected = new IllegalStateException("generate guard failed");
        stubThroughPersistSearch(ragQuestion, ragSearch, searchResult);
        when(ragOrchestrator.generate(QUESTION, searchResult)).thenThrow(expected);

        IllegalStateException actual = assertThrows(
                IllegalStateException.class,
                () -> service.execute(QUESTION));

        assertSame(expected, actual);
        verify(ragPersistenceService, never()).markQuestionFailed(any(), any(), any());
        verify(ragPersistenceService, never()).persistAnswer(any(), any(), anyList());
    }

    @Test
    void propagatesPersistAnswerFailure() {
        RagQuestion ragQuestion = org.mockito.Mockito.mock(RagQuestion.class);
        RagSearch ragSearch = org.mockito.Mockito.mock(RagSearch.class);
        RagSearchOrchestrationResult searchResult = sufficientSearchResult();
        RagGenerationOrchestrationResult generationResult = generationResult();
        IllegalStateException expected = new IllegalStateException("answer persistence failed");
        stubThroughPersistSearch(ragQuestion, ragSearch, searchResult);
        when(ragOrchestrator.generate(QUESTION, searchResult)).thenReturn(generationResult);
        org.mockito.Mockito.doThrow(expected).when(ragPersistenceService)
                .persistAnswer(ragSearch, "답변", List.of(201L));

        IllegalStateException actual = assertThrows(
                IllegalStateException.class,
                () -> service.execute(QUESTION));

        assertSame(expected, actual);
        verify(ragPersistenceService).markQuestionProcessing(ragQuestion);
        verify(ragPersistenceService, never()).markQuestionAnswered(any());
    }

    @Test
    void executesCompleteFlowInRequiredOrder() {
        RagSearchPlan plan = plan();
        CurrentUserContext currentUser = currentUser();
        RagQuestion ragQuestion = org.mockito.Mockito.mock(RagQuestion.class);
        RagSearch ragSearch = org.mockito.Mockito.mock(RagSearch.class);
        RagSearchOrchestrationResult searchResult = sufficientSearchResult();
        RagGenerationOrchestrationResult generationResult = generationResult();
        when(ragQueryService.prepareSearch(QUESTION)).thenReturn(Optional.of(plan));
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser);
        when(ragPersistenceService.persistQuestion(QUESTION, 1001L)).thenReturn(ragQuestion);
        when(ragOrchestrator.search(QUESTION, plan.allowedDocumentVersionIds(), "provider", "model"))
                .thenReturn(searchResult);
        when(ragPersistenceService.persistSearch(same(ragQuestion), eq(10L), anyList()))
                .thenReturn(ragSearch);
        when(ragOrchestrator.generate(QUESTION, searchResult)).thenReturn(generationResult);

        RagQueryResult result = service.execute(QUESTION);

        InOrder order = inOrder(
                ragQueryService,
                currentUserProvider,
                ragPersistenceService,
                ragOrchestrator);
        order.verify(ragQueryService).prepareSearch(QUESTION);
        order.verify(currentUserProvider).getCurrentUser();
        order.verify(ragPersistenceService).persistQuestion(QUESTION, 1001L);
        order.verify(ragPersistenceService).markQuestionProcessing(ragQuestion);
        order.verify(ragOrchestrator).search(
                QUESTION, plan.allowedDocumentVersionIds(), "provider", "model");
        ArgumentCaptor<List<RagSearchPersistenceItem>> captor = persistenceItemsCaptor();
        order.verify(ragPersistenceService).persistSearch(
                same(ragQuestion), eq(plan.aiModelId()), captor.capture());
        order.verify(ragOrchestrator).generate(QUESTION, searchResult);
        order.verify(ragPersistenceService).persistAnswer(
                same(ragSearch), eq("답변"), eq(List.of(201L)));
        order.verify(ragPersistenceService).markQuestionAnswered(ragQuestion);
        assertEquals(List.of(new RagSearchPersistenceItem(201L, 101L, 0.81)), captor.getValue());
        assertEquals(new RagQueryResult(true, "답변", List.of(201L)), result);
    }

    private void stubQuestionPersistence(RagQuestion ragQuestion) {
        when(ragQueryService.prepareSearch(QUESTION)).thenReturn(Optional.of(plan()));
        when(currentUserProvider.getCurrentUser()).thenReturn(currentUser());
        when(ragPersistenceService.persistQuestion(QUESTION, 1001L)).thenReturn(ragQuestion);
    }

    private void stubThroughPersistSearch(
            RagQuestion ragQuestion,
            RagSearch ragSearch,
            RagSearchOrchestrationResult searchResult) {
        stubQuestionPersistence(ragQuestion);
        when(ragOrchestrator.search(QUESTION, Set.of(1L, 2L), "provider", "model"))
                .thenReturn(searchResult);
        when(ragPersistenceService.persistSearch(same(ragQuestion), eq(10L), anyList()))
                .thenReturn(ragSearch);
    }

    private RagSearchPlan plan() {
        return new RagSearchPlan(10L, Set.of(1L, 2L), "provider", "model");
    }

    private CurrentUserContext currentUser() {
        return new CurrentUserContext(1001L, 2001L, Set.of(), null, null, null);
    }

    private RagSearchOrchestrationResult sufficientSearchResult() {
        return new RagSearchOrchestrationResult(
                true,
                List.of(searchItem(101L, 201L, "evidence", 0.81)));
    }

    private RagGenerationOrchestrationResult generationResult() {
        return new RagGenerationOrchestrationResult("답변", List.of(201L));
    }

    private AiRagSearchResultItem searchItem(
            Long documentVersionId,
            Long chunkId,
            String content,
            double similarityScore) {
        return new AiRagSearchResultItem(
                documentVersionId,
                chunkId,
                content,
                similarityScore);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ArgumentCaptor<List<RagSearchPersistenceItem>> persistenceItemsCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}
