package com.teamproject.japan_newhire_rag_backend.rag.persistence.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;
import com.teamproject.japan_newhire_rag_backend.document.chunk.repository.DocumentChunkRepository;
import com.teamproject.japan_newhire_rag_backend.document.entity.Document;
import com.teamproject.japan_newhire_rag_backend.document.version.entity.DocumentVersion;
import com.teamproject.japan_newhire_rag_backend.document.version.repository.DocumentVersionRepository;
import com.teamproject.japan_newhire_rag_backend.rag.model.entity.AiModel;
import com.teamproject.japan_newhire_rag_backend.rag.model.repository.AiModelRepository;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagAnswer;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagCitation;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagQuestion;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagSearch;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagSearchResult;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.repository.RagAnswerRepository;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.repository.RagCitationRepository;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.repository.RagQuestionRepository;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.repository.RagSearchRepository;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.repository.RagSearchResultRepository;

@ExtendWith(MockitoExtension.class)
class RagPersistenceServiceTest {

    @Mock
    private RagQuestionRepository ragQuestionRepository;
    @Mock
    private RagSearchRepository ragSearchRepository;
    @Mock
    private RagSearchResultRepository ragSearchResultRepository;
    @Mock
    private RagAnswerRepository ragAnswerRepository;
    @Mock
    private RagCitationRepository ragCitationRepository;
    @Mock
    private AiModelRepository aiModelRepository;
    @Mock
    private DocumentChunkRepository documentChunkRepository;
    @Mock
    private DocumentVersionRepository documentVersionRepository;

    private RagPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new RagPersistenceService(
                ragQuestionRepository,
                ragSearchRepository,
                ragSearchResultRepository,
                ragAnswerRepository,
                ragCitationRepository,
                aiModelRepository,
                documentChunkRepository,
                documentVersionRepository);
    }

    @Test
    void persistsQuestionWithExactValuesAndReturnsSavedEntity() {
        RagQuestion savedQuestion = org.mockito.Mockito.mock(RagQuestion.class);
        when(ragQuestionRepository.save(any(RagQuestion.class))).thenReturn(savedQuestion);

        RagQuestion result = service.persistQuestion("휴가 규정", 7L);

        ArgumentCaptor<RagQuestion> captor = ArgumentCaptor.forClass(RagQuestion.class);
        verify(ragQuestionRepository).save(captor.capture());
        assertEquals("휴가 규정", captor.getValue().getQuestionText());
        assertEquals(7L, captor.getValue().getCreatedBy());
        assertEquals("PENDING", captor.getValue().getProcessingStatus());
        assertSame(savedQuestion, result);
    }

    @Test
    void persistsSearchResultsInInputOrderWithReferencesAndScores() {
        RagQuestion ragQuestion = org.mockito.Mockito.mock(RagQuestion.class);
        AiModel aiModel = org.mockito.Mockito.mock(AiModel.class);
        DocumentChunk firstChunk = org.mockito.Mockito.mock(DocumentChunk.class);
        DocumentChunk secondChunk = org.mockito.Mockito.mock(DocumentChunk.class);
        DocumentVersion firstVersion = org.mockito.Mockito.mock(DocumentVersion.class);
        DocumentVersion secondVersion = org.mockito.Mockito.mock(DocumentVersion.class);
        when(aiModelRepository.getReferenceById(9L)).thenReturn(aiModel);
        when(ragSearchRepository.save(any(RagSearch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(documentChunkRepository.getReferenceById(101L)).thenReturn(firstChunk);
        when(documentChunkRepository.getReferenceById(102L)).thenReturn(secondChunk);
        when(documentVersionRepository.getReferenceById(201L)).thenReturn(firstVersion);
        when(documentVersionRepository.getReferenceById(202L)).thenReturn(secondVersion);
        List<RagSearchPersistenceItem> items = List.of(
                new RagSearchPersistenceItem(101L, 201L, 0.91),
                new RagSearchPersistenceItem(102L, 202L, 0.82));

        RagSearch result = service.persistSearch(ragQuestion, 9L, items);

        verify(aiModelRepository).getReferenceById(9L);
        assertSame(ragQuestion, result.getRagQuestion());
        assertSame(aiModel, result.getAiModel());
        List<RagSearchResult> savedResults = captureSearchResults();
        assertEquals(2, savedResults.size());
        assertSearchResult(savedResults.get(0), result, firstChunk, firstVersion, 0.91, 1);
        assertSearchResult(savedResults.get(1), result, secondChunk, secondVersion, 0.82, 2);
        verify(documentChunkRepository).getReferenceById(101L);
        verify(documentChunkRepository).getReferenceById(102L);
        verify(documentVersionRepository).getReferenceById(201L);
        verify(documentVersionRepository).getReferenceById(202L);
    }

    @Test
    void persistsSearchWithEmptyResultList() {
        RagQuestion ragQuestion = org.mockito.Mockito.mock(RagQuestion.class);
        AiModel aiModel = org.mockito.Mockito.mock(AiModel.class);
        when(aiModelRepository.getReferenceById(9L)).thenReturn(aiModel);
        when(ragSearchRepository.save(any(RagSearch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RagSearch result = service.persistSearch(ragQuestion, 9L, List.of());

        assertSame(ragQuestion, result.getRagQuestion());
        assertSame(aiModel, result.getAiModel());
        assertEquals(List.of(), captureSearchResults());
    }

    @Test
    void persistsAnswerAndCitationsInInputOrder() {
        RagSearch ragSearch = org.mockito.Mockito.mock(RagSearch.class);
        DocumentChunk firstChunk = citationChunk(
                101L, "취업규칙", "v1", "제1조", "첫 번째 인용문");
        DocumentChunk secondChunk = citationChunk(
                102L, "휴가규정", "v2", "제2조", "두 번째 인용문");
        when(ragAnswerRepository.save(any(RagAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(documentChunkRepository.findAllWithDocumentAndVersionByDocumentChunkIdIn(
                List.of(101L, 102L)))
                .thenReturn(List.of(secondChunk, firstChunk));

        service.persistAnswer(ragSearch, "답변", List.of(101L, 102L));

        RagAnswer savedAnswer = captureAnswer();
        assertSame(ragSearch, savedAnswer.getRagSearch());
        assertEquals("답변", savedAnswer.getAnswerText());
        List<RagCitation> citations = captureCitations();
        assertEquals(2, citations.size());
        assertCitation(
                citations.get(0), savedAnswer, firstChunk, 1,
                "취업규칙", "v1", "제1조", "첫 번째 인용문");
        assertCitation(
                citations.get(1), savedAnswer, secondChunk, 2,
                "휴가규정", "v2", "제2조", "두 번째 인용문");
        verify(documentChunkRepository)
                .findAllWithDocumentAndVersionByDocumentChunkIdIn(List.of(101L, 102L));
    }

    @Test
    void persistsAnswerWithEmptyCitationList() {
        RagSearch ragSearch = org.mockito.Mockito.mock(RagSearch.class);
        when(ragAnswerRepository.save(any(RagAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.persistAnswer(ragSearch, "답변", List.of());

        RagAnswer savedAnswer = captureAnswer();
        assertSame(ragSearch, savedAnswer.getRagSearch());
        assertEquals("답변", savedAnswer.getAnswerText());
        assertEquals(List.of(), captureCitations());
        verify(documentChunkRepository, never())
                .findAllWithDocumentAndVersionByDocumentChunkIdIn(any());
    }

    @Test
    void preservesDuplicateCitationChunkIdsAtDifferentPositions() {
        RagSearch ragSearch = org.mockito.Mockito.mock(RagSearch.class);
        DocumentChunk chunk = citationChunk(
                101L, "취업규칙", "v1", "제1조", "중복 인용문");
        when(ragAnswerRepository.save(any(RagAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(documentChunkRepository.findAllWithDocumentAndVersionByDocumentChunkIdIn(
                List.of(101L, 101L)))
                .thenReturn(List.of(chunk));

        service.persistAnswer(ragSearch, "답변", List.of(101L, 101L));

        RagAnswer savedAnswer = captureAnswer();
        List<RagCitation> citations = captureCitations();
        assertEquals(2, citations.size());
        assertCitation(
                citations.get(0), savedAnswer, chunk, 1,
                "취업규칙", "v1", "제1조", "중복 인용문");
        assertCitation(
                citations.get(1), savedAnswer, chunk, 2,
                "취업규칙", "v1", "제1조", "중복 인용문");
        verify(documentChunkRepository)
                .findAllWithDocumentAndVersionByDocumentChunkIdIn(List.of(101L, 101L));
    }

    @Test
    void persistsNullArticleNumberSnapshot() {
        RagSearch ragSearch = mock(RagSearch.class);
        DocumentChunk chunk = citationChunk(
                101L, "취업규칙", "v1", null, "인용문");
        when(ragAnswerRepository.save(any(RagAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(documentChunkRepository.findAllWithDocumentAndVersionByDocumentChunkIdIn(
                List.of(101L)))
                .thenReturn(List.of(chunk));

        service.persistAnswer(ragSearch, "답변", List.of(101L));

        RagCitation citation = captureCitations().get(0);
        assertEquals(null, citation.getArticleNumberSnapshot());
        assertEquals("인용문", citation.getCitedText());
    }

    @Test
    void failsClosedWhenRequestedCitationChunkIsMissing() {
        RagSearch ragSearch = mock(RagSearch.class);
        DocumentChunk firstChunk = citationChunk(
                101L, "취업규칙", "v1", "제1조", "인용문");
        when(ragAnswerRepository.save(any(RagAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(documentChunkRepository.findAllWithDocumentAndVersionByDocumentChunkIdIn(
                List.of(101L, 102L)))
                .thenReturn(List.of(firstChunk));

        assertThrows(
                IllegalStateException.class,
                () -> service.persistAnswer(ragSearch, "답변", List.of(101L, 102L)));

        verifyNoInteractions(ragCitationRepository);
    }

    @Test
    void marksQuestionProcessingAndSavesExplicitly() {
        RagQuestion question = RagQuestion.create("질문", 1L);

        service.markQuestionProcessing(question);

        assertEquals("PROCESSING", question.getProcessingStatus());
        verify(ragQuestionRepository).save(question);
    }

    @Test
    void marksQuestionAnsweredClearsFailureAndSavesExplicitly() {
        RagQuestion question = RagQuestion.create("질문", 1L);
        question.markFailed("API_ERROR", "실패");

        service.markQuestionAnswered(question);

        assertEquals("ANSWERED", question.getProcessingStatus());
        assertEquals(null, question.getFailureType());
        assertEquals(null, question.getFailureReason());
        verify(ragQuestionRepository).save(question);
    }

    @Test
    void marksQuestionRejectedWithFailureAndSavesExplicitly() {
        RagQuestion question = RagQuestion.create("질문", 1L);

        service.markQuestionRejected(question, "LOW_SIMILARITY", "근거 부족");

        assertEquals("REJECTED", question.getProcessingStatus());
        assertEquals("LOW_SIMILARITY", question.getFailureType());
        assertEquals("근거 부족", question.getFailureReason());
        verify(ragQuestionRepository).save(question);
    }

    @Test
    void marksQuestionFailedWithFailureAndSavesExplicitly() {
        RagQuestion question = RagQuestion.create("질문", 1L);

        service.markQuestionFailed(question, "API_ERROR", "외부 서비스 실패");

        assertEquals("FAILED", question.getProcessingStatus());
        assertEquals("API_ERROR", question.getFailureType());
        assertEquals("외부 서비스 실패", question.getFailureReason());
        verify(ragQuestionRepository).save(question);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<RagSearchResult> captureSearchResults() {
        ArgumentCaptor<Iterable> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(ragSearchResultRepository).saveAll(captor.capture());
        List<RagSearchResult> results = new ArrayList<>();
        captor.getValue().forEach(value -> results.add((RagSearchResult) value));
        return results;
    }

    private RagAnswer captureAnswer() {
        ArgumentCaptor<RagAnswer> captor = ArgumentCaptor.forClass(RagAnswer.class);
        verify(ragAnswerRepository).save(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<RagCitation> captureCitations() {
        ArgumentCaptor<Iterable> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(ragCitationRepository).saveAll(captor.capture());
        List<RagCitation> citations = new ArrayList<>();
        captor.getValue().forEach(value -> citations.add((RagCitation) value));
        return citations;
    }

    private void assertSearchResult(
            RagSearchResult result,
            RagSearch ragSearch,
            DocumentChunk documentChunk,
            DocumentVersion documentVersion,
            double similarityScore,
            int rank) {
        assertSame(ragSearch, result.getRagSearch());
        assertSame(documentChunk, result.getDocumentChunk());
        assertSame(documentVersion, result.getDocumentVersion());
        assertEquals(similarityScore, result.getSimilarityScore());
        assertEquals(rank, result.getResultRank());
    }

    private void assertCitation(
            RagCitation citation,
            RagAnswer ragAnswer,
            DocumentChunk documentChunk,
            int position,
            String documentName,
            String versionName,
            String articleNumber,
            String citedText) {
        assertSame(ragAnswer, citation.getRagAnswer());
        assertSame(documentChunk, citation.getDocumentChunk());
        assertEquals(position, citation.getPosition());
        assertEquals(documentName, citation.getDocumentNameSnapshot());
        assertEquals(versionName, citation.getVersionNameSnapshot());
        assertEquals(articleNumber, citation.getArticleNumberSnapshot());
        assertEquals(citedText, citation.getCitedText());
    }

    private DocumentChunk citationChunk(
            Long chunkId,
            String documentName,
            String versionName,
            String articleNumber,
            String chunkContent) {
        Document document = mock(Document.class);
        when(document.getDocumentName()).thenReturn(documentName);
        DocumentVersion documentVersion = mock(DocumentVersion.class);
        when(documentVersion.getDocument()).thenReturn(document);
        when(documentVersion.getVersionName()).thenReturn(versionName);
        DocumentChunk chunk = mock(DocumentChunk.class);
        when(chunk.getDocumentChunkId()).thenReturn(chunkId);
        when(chunk.getDocumentVersion()).thenReturn(documentVersion);
        when(chunk.getArticleNumber()).thenReturn(articleNumber);
        when(chunk.getChunkContent()).thenReturn(chunkContent);
        return chunk;
    }
}
