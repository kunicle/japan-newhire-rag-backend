package com.teamproject.japan_newhire_rag_backend.rag.persistence.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;
import com.teamproject.japan_newhire_rag_backend.document.chunk.repository.DocumentChunkRepository;
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

@Service
@Transactional
public class RagPersistenceService {

    private final RagQuestionRepository ragQuestionRepository;
    private final RagSearchRepository ragSearchRepository;
    private final RagSearchResultRepository ragSearchResultRepository;
    private final RagAnswerRepository ragAnswerRepository;
    private final RagCitationRepository ragCitationRepository;
    private final AiModelRepository aiModelRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentVersionRepository documentVersionRepository;

    public RagPersistenceService(
            RagQuestionRepository ragQuestionRepository,
            RagSearchRepository ragSearchRepository,
            RagSearchResultRepository ragSearchResultRepository,
            RagAnswerRepository ragAnswerRepository,
            RagCitationRepository ragCitationRepository,
            AiModelRepository aiModelRepository,
            DocumentChunkRepository documentChunkRepository,
            DocumentVersionRepository documentVersionRepository) {
        this.ragQuestionRepository = ragQuestionRepository;
        this.ragSearchRepository = ragSearchRepository;
        this.ragSearchResultRepository = ragSearchResultRepository;
        this.ragAnswerRepository = ragAnswerRepository;
        this.ragCitationRepository = ragCitationRepository;
        this.aiModelRepository = aiModelRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.documentVersionRepository = documentVersionRepository;
    }

    public RagQuestion persistQuestion(String questionText, Long createdByAppUserId) {
        RagQuestion ragQuestion = RagQuestion.create(questionText, createdByAppUserId);
        return ragQuestionRepository.save(ragQuestion);
    }

    public void markQuestionProcessing(RagQuestion ragQuestion) {
        ragQuestion.markProcessing();
        ragQuestionRepository.save(ragQuestion);
    }

    public void markQuestionAnswered(RagQuestion ragQuestion) {
        ragQuestion.markAnswered();
        ragQuestionRepository.save(ragQuestion);
    }

    public void markQuestionRejected(
            RagQuestion ragQuestion,
            String failureType,
            String failureReason) {
        ragQuestion.markRejected(failureType, failureReason);
        ragQuestionRepository.save(ragQuestion);
    }

    public void markQuestionFailed(
            RagQuestion ragQuestion,
            String failureType,
            String failureReason) {
        ragQuestion.markFailed(failureType, failureReason);
        ragQuestionRepository.save(ragQuestion);
    }

    public RagSearch persistSearch(
            RagQuestion ragQuestion,
            Long aiModelId,
            List<RagSearchPersistenceItem> searchResults) {
        AiModel aiModel = aiModelRepository.getReferenceById(aiModelId);
        RagSearch ragSearch = ragSearchRepository.save(RagSearch.create(ragQuestion, aiModel));

        List<RagSearchResult> results = new ArrayList<>();
        int rank = 1;
        for (RagSearchPersistenceItem item : searchResults) {
            DocumentChunk documentChunk =
                    documentChunkRepository.getReferenceById(item.documentChunkId());
            DocumentVersion documentVersion =
                    documentVersionRepository.getReferenceById(item.documentVersionId());
            results.add(RagSearchResult.create(
                    ragSearch, documentChunk, documentVersion, item.similarityScore(), rank));
            rank++;
        }
        ragSearchResultRepository.saveAll(results);

        return ragSearch;
    }

    public void persistAnswer(
            RagSearch ragSearch,
            String answerText,
            List<Long> validCitedChunkIds) {
        RagAnswer ragAnswer = ragAnswerRepository.save(RagAnswer.create(ragSearch, answerText));

        if (validCitedChunkIds.isEmpty()) {
            ragCitationRepository.saveAll(List.of());
            return;
        }

        Map<Long, DocumentChunk> chunksById = documentChunkRepository
                .findAllWithDocumentAndVersionByDocumentChunkIdIn(validCitedChunkIds)
                .stream()
                .collect(Collectors.toMap(
                        DocumentChunk::getDocumentChunkId,
                        Function.identity()));

        List<RagCitation> citations = new ArrayList<>();
        int position = 1;
        for (Long chunkId : validCitedChunkIds) {
            DocumentChunk documentChunk = chunksById.get(chunkId);
            if (documentChunk == null) {
                throw new IllegalStateException("검증된 인용 chunk를 찾을 수 없습니다: " + chunkId);
            }
            DocumentVersion documentVersion = documentChunk.getDocumentVersion();
            citations.add(RagCitation.create(
                    ragAnswer,
                    documentChunk,
                    position,
                    documentVersion.getDocument().getDocumentName(),
                    documentVersion.getVersionName(),
                    documentChunk.getArticleNumber(),
                    documentChunk.getChunkContent()));
            position++;
        }
        ragCitationRepository.saveAll(citations);
    }
}
