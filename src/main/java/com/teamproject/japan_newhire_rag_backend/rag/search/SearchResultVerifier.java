package com.teamproject.japan_newhire_rag_backend.rag.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.teamproject.japan_newhire_rag_backend.document.chunk.entity.DocumentChunk;
import com.teamproject.japan_newhire_rag_backend.document.chunk.repository.DocumentChunkRepository;
import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagSearchResultItem;

public class SearchResultVerifier {

    private final DocumentChunkRepository documentChunkRepository;

    public SearchResultVerifier(DocumentChunkRepository documentChunkRepository) {
        this.documentChunkRepository = documentChunkRepository;
    }

    public List<AiRagSearchResultItem> filterByAllowedDocumentVersions(
            List<AiRagSearchResultItem> searchResults,
            Set<Long> allowedDocumentVersionIds) {
        if (searchResults == null) {
            throw new IllegalArgumentException("검색 결과 목록이 없습니다.");
        }
        if (allowedDocumentVersionIds == null) {
            throw new IllegalArgumentException("허용된 문서 버전 목록이 없습니다.");
        }

        List<AiRagSearchResultItem> membershipCandidates = new ArrayList<>();
        for (AiRagSearchResultItem searchResult : searchResults) {
            if (allowedDocumentVersionIds.contains(searchResult.documentVersionId())) {
                membershipCandidates.add(searchResult);
            }
        }

        if (membershipCandidates.isEmpty()) {
            return List.of();
        }

        Set<Long> chunkIds = membershipCandidates.stream()
                .map(AiRagSearchResultItem::chunkId)
                .collect(Collectors.toSet());
        Map<Long, DocumentChunk> chunksById = documentChunkRepository.findAllById(chunkIds)
                .stream()
                .collect(Collectors.toMap(DocumentChunk::getDocumentChunkId, chunk -> chunk));

        List<AiRagSearchResultItem> verifiedResults = new ArrayList<>();
        for (AiRagSearchResultItem candidate : membershipCandidates) {
            DocumentChunk chunk = chunksById.get(candidate.chunkId());
            if (chunk == null) {
                continue;
            }

            Long actualDocumentVersionId = chunk.getDocumentVersion().getDocumentVersionId();
            if (!actualDocumentVersionId.equals(candidate.documentVersionId())) {
                continue;
            }

            verifiedResults.add(new AiRagSearchResultItem(
                    actualDocumentVersionId,
                    candidate.chunkId(),
                    chunk.getChunkContent(),
                    candidate.similarityScore()));
        }

        return verifiedResults;
    }
}
