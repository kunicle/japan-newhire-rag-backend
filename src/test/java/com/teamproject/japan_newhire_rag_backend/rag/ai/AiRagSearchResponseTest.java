package com.teamproject.japan_newhire_rag_backend.rag.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class AiRagSearchResponseTest {

    @Test
    void createsResponseWithValidSearchResults() {
        AiRagSearchResultItem item = createSearchResult();
        AiRagSearchResponse response = new AiRagSearchResponse(List.of(item));

        assertEquals(List.of(item), response.searchResults());
    }

    @Test
    void rejectsNullSearchResults() {
        assertThrows(IllegalArgumentException.class, () -> new AiRagSearchResponse(null));
    }

    @Test
    void allowsEmptySearchResults() {
        AiRagSearchResponse response = new AiRagSearchResponse(List.of());

        assertEquals(List.of(), response.searchResults());
    }

    private AiRagSearchResultItem createSearchResult() {
        return new AiRagSearchResultItem(1L, 10L, "휴가 규정", 0.9);
    }
}
