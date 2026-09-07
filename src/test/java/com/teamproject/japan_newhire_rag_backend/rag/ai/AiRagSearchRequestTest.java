package com.teamproject.japan_newhire_rag_backend.rag.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class AiRagSearchRequestTest {

    @Test
    void createsRequestWithValidValues() {
        AiRagSearchRequest request =
                new AiRagSearchRequest(
                        "휴가 규정을 알려주세요.",
                        List.of(1L, 2L),
                        "provider-a",
                        "model-a");

        assertEquals("휴가 규정을 알려주세요.", request.question());
        assertEquals(List.of(1L, 2L), request.allowedDocumentVersionIds());
        assertEquals("provider-a", request.providerName());
        assertEquals("model-a", request.modelName());
    }

    @Test
    void rejectsNullOrBlankQuestion() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchRequest(null, List.of(1L), "provider-a", "model-a"));
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchRequest("", List.of(1L), "provider-a", "model-a"));
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchRequest("   ", List.of(1L), "provider-a", "model-a"));
    }

    @Test
    void rejectsNullAllowedDocumentVersionIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchRequest("휴가 규정", null, "provider-a", "model-a"));
    }

    @Test
    void rejectsEmptyAllowedDocumentVersionIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchRequest(
                        "휴가 규정", List.of(), "provider-a", "model-a"));
    }

    @Test
    void rejectsNullOrBlankProviderName() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchRequest("질문", List.of(1L), null, "model-a"));
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchRequest("질문", List.of(1L), "   ", "model-a"));
    }

    @Test
    void rejectsNullOrBlankModelName() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchRequest("질문", List.of(1L), "provider-a", null));
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagSearchRequest("질문", List.of(1L), "provider-a", "   "));
    }
}
