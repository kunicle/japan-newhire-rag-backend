package com.teamproject.japan_newhire_rag_backend.rag.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.common.error.ErrorCode;
import com.teamproject.japan_newhire_rag_backend.common.exception.BusinessException;
import com.teamproject.japan_newhire_rag_backend.domain.auth.api.CurrentUserContext;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagAnswer;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagCitation;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagQuestion;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.entity.RagSearch;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.repository.RagAnswerRepository;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.repository.RagCitationRepository;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.repository.RagQuestionRepository;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.repository.RagSearchRepository;
import com.teamproject.japan_newhire_rag_backend.rag.persistence.service.RagCitationSnapshot;

@Service
@Transactional(readOnly = true)
public class RagQuestionHistoryService {

    private final RagQuestionRepository ragQuestionRepository;
    private final RagSearchRepository ragSearchRepository;
    private final RagAnswerRepository ragAnswerRepository;
    private final RagCitationRepository ragCitationRepository;

    public RagQuestionHistoryService(
            RagQuestionRepository ragQuestionRepository,
            RagSearchRepository ragSearchRepository,
            RagAnswerRepository ragAnswerRepository,
            RagCitationRepository ragCitationRepository) {
        this.ragQuestionRepository = ragQuestionRepository;
        this.ragSearchRepository = ragSearchRepository;
        this.ragAnswerRepository = ragAnswerRepository;
        this.ragCitationRepository = ragCitationRepository;
    }

    public List<RagQuestionHistoryItem> getQuestionHistory(CurrentUserContext currentUser) {
        return ragQuestionRepository
                .findByCreatedByOrderByCreatedAtDesc(currentUser.appUserId())
                .stream()
                .map(this::toHistoryItem)
                .toList();
    }

    public RagQuestionHistoryDetail getQuestionDetail(
            CurrentUserContext currentUser,
            Long questionId) {
        RagQuestion question = ragQuestionRepository
                .findByRagQuestionIdAndCreatedBy(questionId, currentUser.appUserId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "질문을 찾을 수 없습니다."));

        Optional<RagSearch> search = ragSearchRepository
                .findFirstByRagQuestion_RagQuestionIdOrderBySearchedAtDesc(questionId);
        if (search.isEmpty()) {
            return toDetail(question, null, List.of());
        }

        Optional<RagAnswer> answer = ragAnswerRepository
                .findByRagSearch_RagSearchId(search.get().getRagSearchId());
        if (answer.isEmpty()) {
            return toDetail(question, null, List.of());
        }

        RagAnswer foundAnswer = answer.get();
        List<RagCitationSnapshot> citations = ragCitationRepository
                .findByRagAnswer_RagAnswerIdOrderByPositionAsc(foundAnswer.getRagAnswerId())
                .stream()
                .map(this::toCitationSnapshot)
                .toList();
        return toDetail(question, foundAnswer.getAnswerText(), citations);
    }

    private RagQuestionHistoryItem toHistoryItem(RagQuestion question) {
        return new RagQuestionHistoryItem(
                question.getRagQuestionId(),
                question.getQuestionText(),
                question.getProcessingStatus(),
                question.getCreatedAt());
    }

    private RagQuestionHistoryDetail toDetail(
            RagQuestion question,
            String answer,
            List<RagCitationSnapshot> citations) {
        return new RagQuestionHistoryDetail(
                question.getRagQuestionId(),
                question.getQuestionText(),
                question.getProcessingStatus(),
                question.getCreatedAt(),
                answer,
                citations);
    }

    private RagCitationSnapshot toCitationSnapshot(RagCitation citation) {
        return new RagCitationSnapshot(
                citation.getDocumentChunk().getDocumentChunkId(),
                citation.getDocumentNameSnapshot(),
                citation.getVersionNameSnapshot(),
                citation.getArticleNumberSnapshot(),
                citation.getCitedText());
    }
}
