package com.teamproject.japan_newhire_rag_backend.rag.citation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagSearchResultItem;

public class CitationValidator {

    public List<Long> filterValidCitations(
            List<Long> citedChunkIds,
            List<AiRagSearchResultItem> verifiedSearchResults) {
        if (citedChunkIds == null) {
            throw new IllegalArgumentException("인용된 chunk 목록이 없습니다.");
        }
        if (verifiedSearchResults == null) {
            throw new IllegalArgumentException("검증된 검색 결과 목록이 없습니다.");
        }

        Set<Long> verifiedChunkIds = new HashSet<>();
        for (AiRagSearchResultItem searchResult : verifiedSearchResults) {
            verifiedChunkIds.add(searchResult.chunkId());
        }

        List<Long> validCitations = new ArrayList<>();
        for (Long citedChunkId : citedChunkIds) {
            if (verifiedChunkIds.contains(citedChunkId)) {
                validCitations.add(citedChunkId);
            }
        }

        return validCitations;
    }
}
