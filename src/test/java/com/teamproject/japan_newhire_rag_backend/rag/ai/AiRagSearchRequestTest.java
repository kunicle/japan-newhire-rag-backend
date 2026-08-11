package com.teamproject.japan_newhire_rag_backend.rag.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class AiRagSearchRequestTest {

    @Test
    void createsRequestWithValidValues() {
        AiRagSearchRequest request =
                new AiRagSearchRequest("휴가 규정을 알려주세요.", List.of(1L, 2L));

        assertEquals("휴가 규정을 알려주세요.", request.question());
        assertEquals(List.of(1L, 2L), request.allowedDocumentIds());
    }

    @Test
    void rejectsNullOrBlankQuestion() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchRequest(null, List.of(1L)));
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchRequest("", List.of(1L)));
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchRequest("   ", List.of(1L)));
    }

    @Test
    void rejectsNullAllowedDocumentIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchRequest("휴가 규정", null));
    }

    @Test
    void rejectsEmptyAllowedDocumentIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchRequest("휴가 규정", List.of()));
    }
}
