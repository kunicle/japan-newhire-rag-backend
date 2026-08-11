package com.teamproject.japan_newhire_rag_backend.rag.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagSearchResultItem;

public class SearchResultVerifier {

    public List<AiRagSearchResultItem> filterByAllowedDocuments(
            List<AiRagSearchResultItem> searchResults,
            Set<Long> allowedDocumentIds) {
        if (searchResults == null) {
            throw new IllegalArgumentException("검색 결과 목록이 없습니다.");
        }
        if (allowedDocumentIds == null) {
            throw new IllegalArgumentException("허용된 문서 목록이 없습니다.");
        }

        List<AiRagSearchResultItem> verifiedResults = new ArrayList<>();
        for (AiRagSearchResultItem searchResult : searchResults) {
            if (allowedDocumentIds.contains(searchResult.documentId())) {
                verifiedResults.add(searchResult);
            }
        }

        return verifiedResults;
    }
}
