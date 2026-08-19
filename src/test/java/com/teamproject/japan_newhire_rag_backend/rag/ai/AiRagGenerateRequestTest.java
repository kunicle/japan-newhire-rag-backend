package com.teamproject.japan_newhire_rag_backend.rag.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class AiRagGenerateRequestTest {

    @Test
    void createsRequestWithValidValues() {
        AiRagSearchResultItem evidence = createEvidence();
        AiRagGenerateRequest request =
                new AiRagGenerateRequest("휴가 규정", List.of(evidence));

        assertEquals("휴가 규정", request.question());
        assertEquals(List.of(evidence), request.evidence());
    }

    @Test
    void rejectsNullOrBlankQuestion() {
        List<AiRagSearchResultItem> evidence = List.of(createEvidence());

        assertThrows(IllegalArgumentException.class,
                () -> new AiRagGenerateRequest(null, evidence));
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagGenerateRequest("", evidence));
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagGenerateRequest("   ", evidence));
    }

    @Test
    void rejectsNullEvidence() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagGenerateRequest("휴가 규정", null));
    }

    @Test
    void rejectsEmptyEvidence() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagGenerateRequest("휴가 규정", List.of()));
    }

    private AiRagSearchResultItem createEvidence() {
        return new AiRagSearchResultItem(1L, 10L, "휴가 규정", 0.9);
    }
}
