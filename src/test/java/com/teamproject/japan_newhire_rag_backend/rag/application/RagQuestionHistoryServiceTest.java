package com.teamproject.japan_newhire_rag_backend.rag.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.domain.auth.enums.RoleType;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagAnswer;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagCitation;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagQuestion;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagSearch;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.repository.RagAnswerRepository;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.repository.RagCitationRepository;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.repository.RagQuestionRepository;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.repository.RagSearchRepository;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.service.RagCitationSnapshot;

@ExtendWith(MockitoExtension.class)
class RagQuestionHistoryServiceTest {

    private static final Long APP_USER_ID = 1001L;
    private static final CurrentUserContext CURRENT_USER = new CurrentUserContext(
            APP_USER_ID, 2001L, Set.of(RoleType.EMPLOYEE), 10L, 1, null);

    @Mock RagQuestionRepository ragQuestionRepository;
    @Mock RagSearchRepository ragSearchRepository;
    @Mock RagAnswerRepository ragAnswerRepository;
    @Mock RagCitationRepository ragCitationRepository;

    private RagQuestionHistoryService service;

    @BeforeEach
    void setUp() {
        service = new RagQuestionHistoryService(
                ragQuestionRepository,
                ragSearchRepository,
                ragAnswerRepository,
                ragCitationRepository);
    }

    @Test
    void returnsQuestionHistoryInRepositoryOrderUsingAppUserId() {
        LocalDateTime newerAt = LocalDateTime.of(2026, 8, 19, 11, 0);
        LocalDateTime olderAt = LocalDateTime.of(2026, 8, 18, 10, 0);
        RagQuestion newer = question(20L, "최근 질문", "ANSWERED", newerAt);
        RagQuestion older = question(10L, "이전 질문", "FAILED", olderAt);
        when(ragQuestionRepository.findByCreatedByOrderByCreatedAtDesc(APP_USER_ID))
                .thenReturn(List.of(newer, older));

        List<RagQuestionHistoryItem> result = service.getQuestionHistory(CURRENT_USER);

        assertEquals(List.of(
                new RagQuestionHistoryItem(20L, "최근 질문", "ANSWERED", newerAt),
                new RagQuestionHistoryItem(10L, "이전 질문", "FAILED", olderAt)), result);
        verify(ragQuestionRepository).findByCreatedByOrderByCreatedAtDesc(APP_USER_ID);
        verifyNoInteractions(ragSearchRepository, ragAnswerRepository, ragCitationRepository);
    }

    @Test
    void returnsEmptyHistoryWithoutLookingUpDetails() {
        when(ragQuestionRepository.findByCreatedByOrderByCreatedAtDesc(APP_USER_ID))
                .thenReturn(List.of());

        List<RagQuestionHistoryItem> result = service.getQuestionHistory(CURRENT_USER);

        assertEquals(List.of(), result);
        verifyNoInteractions(ragSearchRepository, ragAnswerRepository, ragCitationRepository);
    }

    @Test
    void returnsAnsweredDetailWithCitationSnapshotsInRepositoryOrder() {
        LocalDateTime askedAt = LocalDateTime.of(2026, 8, 19, 10, 0);
        RagQuestion question = question(10L, "연차는 몇 일인가요?", "ANSWERED", askedAt);
        RagSearch search = mock(RagSearch.class);
        RagAnswer answer = mock(RagAnswer.class);
        when(search.getRagSearchId()).thenReturn(30L);
        when(answer.getRagAnswerId()).thenReturn(40L);
        when(answer.getAnswerText()).thenReturn("연차는 연 15일입니다.");
        RagCitation first = citation(101L, "취업규칙", "v1", "제5조", "첫 번째 근거");
        RagCitation second = citation(102L, "휴가규정", "v2", null, "두 번째 근거");
        when(ragQuestionRepository.findByRagQuestionIdAndCreatedBy(10L, APP_USER_ID))
                .thenReturn(Optional.of(question));
        when(ragSearchRepository.findFirstByRagQuestion_RagQuestionIdOrderBySearchedAtDesc(10L))
                .thenReturn(Optional.of(search));
        when(ragAnswerRepository.findByRagSearch_RagSearchId(30L))
                .thenReturn(Optional.of(answer));
        when(ragCitationRepository.findByRagAnswer_RagAnswerIdOrderByPositionAsc(40L))
                .thenReturn(List.of(first, second));

        RagQuestionHistoryDetail result = service.getQuestionDetail(CURRENT_USER, 10L);

        assertEquals(10L, result.questionId());
        assertEquals("연차는 몇 일인가요?", result.question());
        assertEquals("ANSWERED", result.status());
        assertEquals(askedAt, result.askedAt());
        assertEquals("연차는 연 15일입니다.", result.answer());
        assertEquals(List.of(
                new RagCitationSnapshot(101L, "취업규칙", "v1", "제5조", "첫 번째 근거"),
                new RagCitationSnapshot(102L, "휴가규정", "v2", null, "두 번째 근거")),
                result.citations());
        assertNull(result.citations().get(1).articleNumber());
        InOrder order = inOrder(
                ragQuestionRepository,
                ragSearchRepository,
                ragAnswerRepository,
                ragCitationRepository);
        order.verify(ragQuestionRepository).findByRagQuestionIdAndCreatedBy(10L, APP_USER_ID);
        order.verify(ragSearchRepository)
                .findFirstByRagQuestion_RagQuestionIdOrderBySearchedAtDesc(10L);
        order.verify(ragAnswerRepository).findByRagSearch_RagSearchId(30L);
        order.verify(ragCitationRepository)
                .findByRagAnswer_RagAnswerIdOrderByPositionAsc(40L);
    }

    @Test
    void returnsNonAnsweredDetailWithoutAnswerLookupWhenSearchIsMissing() {
        RagQuestion question = question(
                11L, "처리 중 질문", "PROCESSING", LocalDateTime.of(2026, 8, 19, 12, 0));
        when(ragQuestionRepository.findByRagQuestionIdAndCreatedBy(11L, APP_USER_ID))
                .thenReturn(Optional.of(question));
        when(ragSearchRepository.findFirstByRagQuestion_RagQuestionIdOrderBySearchedAtDesc(11L))
                .thenReturn(Optional.empty());

        RagQuestionHistoryDetail result = service.getQuestionDetail(CURRENT_USER, 11L);

        assertEquals("PROCESSING", result.status());
        assertNull(result.answer());
        assertEquals(List.of(), result.citations());
        verifyNoInteractions(ragAnswerRepository, ragCitationRepository);
    }

    @Test
    void returnsNonAnsweredDetailWithoutCitationLookupWhenAnswerIsMissing() {
        RagQuestion question = question(
                12L, "실패 질문", "FAILED", LocalDateTime.of(2026, 8, 19, 13, 0));
        RagSearch search = mock(RagSearch.class);
        when(search.getRagSearchId()).thenReturn(31L);
        when(ragQuestionRepository.findByRagQuestionIdAndCreatedBy(12L, APP_USER_ID))
                .thenReturn(Optional.of(question));
        when(ragSearchRepository.findFirstByRagQuestion_RagQuestionIdOrderBySearchedAtDesc(12L))
                .thenReturn(Optional.of(search));
        when(ragAnswerRepository.findByRagSearch_RagSearchId(31L))
                .thenReturn(Optional.empty());

        RagQuestionHistoryDetail result = service.getQuestionDetail(CURRENT_USER, 12L);

        assertEquals("FAILED", result.status());
        assertNull(result.answer());
        assertTrue(result.citations().isEmpty());
        verifyNoInteractions(ragCitationRepository);
    }

    @Test
    void hidesForeignAndMissingQuestionsWithResourceNotFound() {
        when(ragQuestionRepository.findByRagQuestionIdAndCreatedBy(99L, APP_USER_ID))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getQuestionDetail(CURRENT_USER, 99L));

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
        assertEquals("질문을 찾을 수 없습니다.", exception.getMessage());
        verify(ragQuestionRepository).findByRagQuestionIdAndCreatedBy(99L, APP_USER_ID);
        verifyNoInteractions(ragSearchRepository, ragAnswerRepository, ragCitationRepository);
    }

    private RagQuestion question(
            Long questionId,
            String text,
            String status,
            LocalDateTime createdAt) {
        RagQuestion question = mock(RagQuestion.class);
        when(question.getRagQuestionId()).thenReturn(questionId);
        when(question.getQuestionText()).thenReturn(text);
        when(question.getProcessingStatus()).thenReturn(status);
        when(question.getCreatedAt()).thenReturn(createdAt);
        return question;
    }

    private RagCitation citation(
            Long chunkId,
            String documentName,
            String versionName,
            String articleNumber,
            String citedText) {
        DocumentChunk chunk = mock(DocumentChunk.class);
        when(chunk.getDocumentChunkId()).thenReturn(chunkId);
        RagCitation citation = mock(RagCitation.class);
        when(citation.getDocumentChunk()).thenReturn(chunk);
        when(citation.getDocumentNameSnapshot()).thenReturn(documentName);
        when(citation.getVersionNameSnapshot()).thenReturn(versionName);
        when(citation.getArticleNumberSnapshot()).thenReturn(articleNumber);
        when(citation.getCitedText()).thenReturn(citedText);
        return citation;
    }
}
