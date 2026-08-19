package com.teamproject.japan_newhire_rag_backend.rag.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class AiRagGenerateResponseTest {

    @Test
    void rejectsNullCitedChunkIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagGenerateResponse("답변", null));
    }

    @Test
    void allowsEmptyCitedChunkIds() {
        AiRagGenerateResponse response = new AiRagGenerateResponse("답변", List.of());

        assertEquals(List.of(), response.citedChunkIds());
    }

    @Test
    void allowsNullAnswer() {
        AiRagGenerateResponse response = new AiRagGenerateResponse(null, List.of(10L));

        assertNull(response.answer());
        assertEquals(List.of(10L), response.citedChunkIds());
    }
}
