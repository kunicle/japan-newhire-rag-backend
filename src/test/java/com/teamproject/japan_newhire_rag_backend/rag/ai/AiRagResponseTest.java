package com.teamproject.japan_newhire_rag_backend.rag.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class AiRagResponseTest {

    @Test
    void createsResponseWithValidValues() {
        AiRagSearchResultItem item = new AiRagSearchResultItem(1L, 10L, "휴가 규정", 0.9);
        AiRagResponse response = new AiRagResponse("휴가는 연 15일입니다.", List.of(item));

        assertEquals("휴가는 연 15일입니다.", response.answer());
        assertEquals(List.of(item), response.searchResults());
    }

    @Test
    void rejectsNullSearchResults() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagResponse("답변", null));
    }

    @Test
    void allowsEmptySearchResults() {
        AiRagResponse response = new AiRagResponse(null, List.of());

        assertEquals(List.of(), response.searchResults());
    }

    @Test
    void allowsNullAnswer() {
        AiRagSearchResultItem item = new AiRagSearchResultItem(1L, 10L, "휴가 규정", 0.9);
        AiRagResponse response = new AiRagResponse(null, List.of(item));

        assertNull(response.answer());
        assertEquals(List.of(item), response.searchResults());
    }
}
