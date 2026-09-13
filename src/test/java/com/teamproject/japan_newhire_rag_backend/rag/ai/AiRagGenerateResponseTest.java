package com.teamproject.japan_newhire_rag_backend.rag.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.teamproject.japan_newhire_rag_backend.rag.RagAnswerStatus;

class AiRagGenerateResponseTest {

    @Test
    void rejectsNullCitedChunkIds() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagGenerateResponse(RagAnswerStatus.ANSWERED, "답변", null));
    }

    @Test
    void allowsEmptyCitedChunkIds() {
        AiRagGenerateResponse response = new AiRagGenerateResponse(
                RagAnswerStatus.ANSWERED, "답변", List.of());

        assertEquals(List.of(), response.citedChunkIds());
    }

    @Test
    void allowsNullAnswer() {
        AiRagGenerateResponse response = new AiRagGenerateResponse(
                RagAnswerStatus.INSUFFICIENT_EVIDENCE, null, List.of(10L));

        assertNull(response.answer());
        assertEquals(List.of(10L), response.citedChunkIds());
    }

    @Test
    void rejectsNullStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagGenerateResponse(null, null, List.of()));
    }

    @Test
    void rejectsBlankAnswerForAnsweredStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiRagGenerateResponse(
                        RagAnswerStatus.ANSWERED, " ", List.of(10L)));
    }
}
