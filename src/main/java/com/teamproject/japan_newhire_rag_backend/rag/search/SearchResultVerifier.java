package com.teamproject.japan_newhire_rag_backend.rag.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.teamproject.japan_newhire_rag_backend.rag.ai.AiRagSearchResultItem;

public class SearchResultVerifier {

    public List<AiRagSearchResultItem> filterByAllowedDocumentVersions(
            List<AiRagSearchResultItem> searchResults,
            Set<Long> allowedDocumentVersionIds) {
        if (searchResults == null) {
            throw new IllegalArgumentException("검색 결과 목록이 없습니다.");
        }
        if (allowedDocumentVersionIds == null) {
            throw new IllegalArgumentException("허용된 문서 버전 목록이 없습니다.");
        }

        List<AiRagSearchResultItem> verifiedResults = new ArrayList<>();
        for (AiRagSearchResultItem searchResult : searchResults) {
            if (allowedDocumentVersionIds.contains(searchResult.documentVersionId())) {
                verifiedResults.add(searchResult);
            }
        }

        return verifiedResults;
    }
}
